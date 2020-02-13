(ns retrieve-p2p-links.core
  (:gen-class)
  (:require
   [clojure.string :as str]
   [clojure.pprint :refer [pprint]]
   [clojure.edn :as edn]
   [clojure.core.async :as async :refer [go chan <!! <! >!! >!]]
   [clojure.data.xml :as xml]
   [clojure.tools.logging :as log :refer [trace debug info warn error fatal]]
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [environ.core :refer [env]]
   [hato.client :as http]
   [signal.handler :refer [on-signal with-handler]]
   [clj-pid.core :as pid]))

;; Default config
(def ^:dynamic *config*
  (atom {
         :history-file "history.edn"
         :user-agent "Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19"
         :pid-file "/var/run/retrieve-p2p-links.pid"
         :connect-timeout 4000
         :keep-alive 120
         :redirect-policy :always
         :magnet-pattern #"magnet:\?xt=urn:btih:[^ \"]+"
         :ed2k-pattern #"ed2k://\|file\|[^ \"]+"
         }))

;; History handled links
(def ^:dynamic *history* (atom {}))

(defn read-config "Update *config* from file"
  []
  (let [config-file (env :retrieve-p2p-links-config "config.edn")]
    (log/info "Reading config file" config-file)
    (swap! *config*
           merge
           (try
             (with-open [r (io/reader config-file)]
               (edn/read (java.io.PushbackReader. r)))
             (catch java.io.IOException e
               (log/errorf "Could not open '%s': %s\n" config-file (.getMessage e))
               (System/exit 1))
             (catch RuntimeException e
               (log/errorf "Error parsing end file '%s': %s\n" config-file (.getMessage e))
               (System/exit 1))))
    (log/debug @*config*)))

(def http-client
  (memoize
   (fn []
     (http/build-http-client
      {:connect-timeout (get @*config* :connect-timeout 4000)
       :redirect-policy (get @*config* :redirect-policy :always)}))))

(defn http-headers
  []
  {"user-agent"
   (:user-agent @*config*)})

(defn http-option
  []
  {:http-client (http-client)
   :headers (http-headers)})

(defn fetch-url
  [url]
  (log/debug "Fetching " url)
  (log/debug "Option: " (http-option))
  (let [res (http/get url (http-option))]
    ;; 2xx Success, 3xx Redirection
    ;; 4xx Client errors, 5xx Server errors
    (if (< (:status res) 300) res
        (throw (ex-info (str "HTTP code " (:status res))
                        res)))))

(defn item-link
  "Return item's link tag content"
  [item]
  (when item
    (->> item
         (filter #(= :link (:tag %)))
         (first)
         (:content)
         (first))))

(defn retrieve-magnet
  "Retrieve magnet links from web page"
  [response]
  (when-let [pat (re-pattern (:magnet-pattern @*config*))]
    (re-seq pat (:body response))))

(defn retrieve-ed2k
  "Retrieve ed2k links from web page"
  [response]
  (when-let [pat (re-pattern (:ed2k-pattern @*config*))]
    (re-seq pat (:body response))))

(defn send-magnet
  "Call external program to handle magnet link, return boolean"
  [magnet]
  (when-let [cmdpat (:magnet-command @*config*)]
    (let [cmdline (map #(format % magnet) cmdpat)]
      (log/info "calling " (str/join " " cmdline))
      (let [{:keys [exit out err]}
            (apply sh cmdline)]
        (= 0 exit)))))

(defn send-ed2k
  "Call external program to handle ed2k link, return boolean"
  [ed2k]
  (when-let [cmdpat (:ed2k-command @*config*)]
    (let [cmdline (map #(format % ed2k) cmdpat)]
      (log/info "calling " (str/join " " cmdline))
      (let [{:keys [exit out err]}
            (apply sh cmdline)]
        (= 0 exit)))))

(defn read-history
  "Read history from file"
  ([path]
   (log/info "Reading history file" path)
   (swap! *history* into
          (try
            (->>
             (slurp path)
             (edn/read-string))
            (catch Exception e))))
  ([]
   (when-let [path (:history-file @*config*)]
     (read-history path))))

(defn save-history
  "Write history to file"
  ([history path]
   (when path
     (log/info "Saving history to file" path)
     (try
       (spit path (prn-str history))
       (catch Exception e
         (log/warn path ":" (.getMessage e))))))
  ([]
   (save-history @*history* (:history-file @*config*))))

(defn start-fetching
  []
  (doseq [[cat rssurl] (:feeds @*config*)]
    (let [items
          (do (log/debug "Fetching and parsing rss: " rssurl)
              (->> (fetch-url rssurl)
                   (:body)
                   (xml/parse-str)
                   (:content)
                   (first)
                   (:content)
                   (filter #(= :item (:tag %)))
                   (map :content)
                   ))

          item-sent
          (->>
           (for [item items]
             (when-let [link (item-link item)]
               (when-not (contains? @*history* link)
                 (do (log/info "Fetching item" link)
                     (when-let [page (fetch-url link)]
                       (if-let [magnet (last (retrieve-magnet page))]
                         (and (send-magnet magnet)
                              [link magnet])
                         (when-let [ed2k (last (retrieve-ed2k page))]
                           (and (send-ed2k ed2k)
                                [link ed2k]))))))))
           (remove nil?))]
      (when (seq item-sent)
        (apply swap! *history* conj item-sent)
        (save-history)))))

;; Signal to trigger fetching
(def fetch-sig :usr1)

;; Signal to trigger saving history
(def save-sig :usr2)

;; main function
(defn -main
  "Retrieve p2p links from website"
  [& args]

  ;; Read config file
  (read-config)

  ;; Read history file
  (read-history)

  ;; Save history file at shutdown
  (.addShutdownHook (Runtime/getRuntime) (Thread. #(save-history)))

  ;; Set http keep alive
  (when-let [keepalive (:keep-alive @*config*)]
    (System/setProperty "jdk.httpclient.keepalivetimeout" (.toString keepalive)))

  ;; Save pid
  (pid/initialize! (get @*config* :pid-file "/var/run/retrieve-p2p-links.pid"))


  (let [ch (chan)]
    ;; Setup signal handlers
    (doseq [sig [fetch-sig save-sig :int :term :hup]]
      (with-handler sig
        (go
          (log/infof "signal %s received" (.toUpperCase (name sig)))
          (>! ch sig))))

    (loop [] ;; waiting for signal triggers
      (log/infof "PID %s is waiting for signal %s to run ..."
                 (pid/current)
                 (.toUpperCase (name fetch-sig)))

      (let [sig (<!! ch)]
        (condp = sig
          fetch-sig (do
                      (try (start-fetching)
                           (catch clojure.lang.ExceptionInfo e
                             (log/warn "Failed fetching url: " (.getMessage e)))
                           (catch Exception e
                             (log/error "Error fetching url: " (.toString e))
                             (System/exit 1)))
                      (recur))

          save-sig (do (save-history)
                       (recur))

          :int nil
          :term nil
          :hup (do (read-config)
                   (recur))
          (log/warn "unknown signal:" sig)))))

  ;; Gracefully shutdown
  (shutdown-agents)
  (log/info "done"))

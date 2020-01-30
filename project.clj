(defproject retrieve-p2p-links "0.1.0"
  :description "Retrieve P2P links from website"
  :url "https://github.com/carlzhc/retrieve-p2p-links"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.logging "0.5.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/core.async "0.7.559"]
                 [spootnik/signal "0.2.4"]
                 [clj-pid "0.1.2"]
                 [hato "0.4.1"]
                 [environ "1.1.0"]]
  :main retrieve-p2p-links.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :plugins [[lein-environ "1.1.0"]
            [carlzhc/lein-tar "3.3.0.1"]
            [lein-rpmbuild "0.1.4-SNAPSHOT"]]
  :tar {:uberjar true
        :format :tar-gz
        :jar-path "/var/lib/retrieve-p2p-links"}
  :war-exclusions [#"^config/"]
  :jar-exclusions [#"^config/"]
  :uberjar-exclusions [#"^config/"]
  :rpmbuild {:%define ["__os_install_post" "%{nil}"]
             :Release "1%{?dist}"
             :Summary "Retrieve P2P links from website"
             :Group "Application"
             :%description
             "Retrieve P2P links from website, and launch external command line program to handle the links.
"
             :%post ["/usr/bin/systemctl daemon-reload"
                     "ln -snf %{name}-%{version}-standalone.jar /var/lib/retrieve-p2p-links/uberjar/%{name}.jar"]
             :%config ["/var/lib/retrieve-p2p-links/config.edn"]
             :%doc ["README.md"]
             :%changelog :gittag})

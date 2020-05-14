divert(-1)
define(`DOMAIN', `website')
define(`WEBSITE',`https://www.DOMAIN.com')
define(`RSSURL', `WEBSITE/feed/')
define(`ENTERTAINMENT', `http://www.DOMAIN.com/entertainment')
define(`MOVIEFEED', `RSSURL?cat=151')
define(`TRANSMISSIONHOME', `/var/lib/transmission')
define(`BTDESTDIR', `TRANSMISSIONHOME/done')
define(`USERAGENT', `Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19')
divert`'dnl
{
 :domain "DOMAIN"
 :website "WEBSITE"
 :rss-url "RSSURL"
 :entertainment "http://www.DOMAIN.com/entertainment"
 :transmission-home "TRANSMISSIONHOME"
 :movie-feed "MOVIEFEED"
 :feeds {:moviefeed "MOVIEFEED"}
 :btdestdir "BTDESTDIR"
 :history-file "history.edn"
 :user-agent "USERAGENT"
 :pid-file ".retrieve-p2p-links.pid"
 :connect-timeout 4000
 :keep-alive 120
 :redirect-policy :always
 :magnet-command ["/bin/echo" "transmission-remote" "-w" "BTDESTDIR" "-a" "%s"]
 :magnet-pattern "magnet:\\?xt=urn:btih:[^ \"]+"
 :ed2k-pattern "ed2k://\\|file\\|[^ \"]+"
 :ed2k-command ["/bin/echo" "/bin/sh" "-ec" "(echo 'dllink %s'; sleep 1) | nc -t localhost 4000"]
}

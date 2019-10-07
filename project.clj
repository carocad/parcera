(defproject carocad/parcera "0.1.2"
  :description "Grammar-based Clojure(script) parser"
  :url "https://github.com/carocad/parcera"
  :license {:name "LGPLv3"
            :url  "https://github.com/carocad/parcera/blob/master/LICENSE.md"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [instaparse/instaparse "1.4.10"]]
  :profiles {:dev {:dependencies [[criterium/criterium "0.4.5"] ;; benchmark
                                  [org.clojure/test.check "0.10.0"]]
                   :plugins      [[jonase/eastwood "0.3.5"]]}}
  :test-selectors {:default     (fn [m] (not (some #{:benchmark} (keys m))))
                   :benchmark   :benchmark}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]])

(defproject carocad/parcera "0.4.0"
  :description "Grammar-based Clojure(script) parser"
  :url "https://github.com/carocad/parcera"
  :license {:name "LGPLv3"
            :url  "https://github.com/carocad/parcera/blob/master/LICENSE.md"}
  :source-paths ["src/clojure" "src/javascript"]
  :java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :profiles {:dev      {:dependencies   [[criterium/criterium "0.4.5"] ;; benchmark
                                         [org.clojure/test.check "0.10.0"]] ;; generative testing
                        :plugins        [[jonase/eastwood "0.3.5"]] ;; linter
                        :resource-paths ["target"]
                        :clean-targets  ^{:protect false} ["target"]}
             ;; java reloader
             ;[lein-virgil "0.1.9"]]
             :provided {:dependencies [[org.antlr/antlr4-runtime "4.7.1"]]}}

  :test-selectors {:default     (fn [m] (not (some #{:benchmark} (keys m))))
                   :benchmark   :benchmark}

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]])

(defproject carocad/parcera "0.4.0"
  :description "Grammar-based Clojure(script) parser"
  :url "https://github.com/carocad/parcera"
  :license {:name "LGPLv3"
            :url  "https://github.com/carocad/parcera/blob/master/LICENSE.md"}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  ;[instaparse/instaparse "1.4.10"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :profiles {:dev      {:dependencies [;; benchmark
                                       [criterium/criterium "0.4.5"]
                                       ;; generative testing
                                       [org.clojure/test.check "0.10.0"]
                                       ;; cljs repl
                                       [com.bhauman/figwheel-main "0.2.3"]]
                        :plugins      [;; linter
                                       [jonase/eastwood "0.3.5"]]}
             ;; java reloader
             ;[lein-virgil "0.1.9"]]
             :provided {:dependencies [[org.clojure/clojurescript "1.10.520"]
                                       [org.antlr/antlr4-runtime "4.7.1"]]}}
  ;:cljsbuild
  #_{:builds
     [{:id           "dev"
       :source-paths ["src/clojure" "src/javascript"]
       :compiler     {:main          parcera.core
                      :target        :nodejs
                      :output-to     "target/dev/index.js"
                      :output-dir    "target/dev/"
                      :infer-externs true
                      :optimizations :none}}
      {:id           "test"
       :source-paths ["src/clojure" "test"]
       :compiler     {:main          parcera.test-runner
                      :output-to     "target/test/main.js"
                      :output-dir    "target/test/"
                      :target        :nodejs
                      :optimizations :none}}]
     :test-commands
     {"test" ["node" "target/test/main.js"]}}
  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main"]}
  :test-selectors {:default     (fn [m] (not (some #{:benchmark} (keys m))))
                   :benchmark   :benchmark}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]])

(defproject parsero "0.1.0-SNAPSHOT"
  :description "Safe Clojure(script) parser that will make you smile"
  :url "https://github.com/carocad/parsero"
  :license {:name "LGPLv3"
            :url  "https://github.com/carocad/parsero/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [instaparse/instaparse "1.4.10"]
                 [org.clojure/test.check "0.10.0"]]
  :profiles {:dev {:dependencies [[criterium/criterium "0.4.5"]];; benchmark]
                   :plugins      [[jonase/eastwood "0.3.5"]]}}
  :test-selectors {:default     (fn [m] (not (some #{:benchmark} (keys m))))
                   :benchmark   :benchmark})

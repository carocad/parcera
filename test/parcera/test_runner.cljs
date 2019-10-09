(ns parcera.test-runner
  (:require [cljs.nodejs :as nodejs]
            [cljs.test :refer-macros [run-tests]]
            [parcera.test.core :as ct]))

(nodejs/enable-util-print!)

(defn -main []
  (run-tests 'parcera.test.core))

(set! *main-cli-fn* -main)

(ns parcera.spec
  "Specifications for the **content** of some Antlr parser rules
  which would otherwise be too difficult to express"
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

;; a name can contain a maximum of 1 /
(def qualified-name #?(:clj  #"^:?:?([^\s\/]+\/)?(\/|[^\s\/]+)$"
                       ;; for some reason cljs doesnt accept escaping the / characters
                       :cljs #"^:?:?([^\s/]+/)?(/|[^\s/]+)$"))


(defn- qualified-name? [text] (re-find qualified-name text))

;; a symbol cannot start with a number
(defn- symbol-number-start? [text] (re-find #"^[+-]?\d+" text))

(s/def ::symbol (s/and qualified-name? (complement symbol-number-start?)))

(s/def ::macro_keyword qualified-name?)


(defn- report
  "utility to avoid repeating this code over and over again"
  [rule children metadata message]
  (with-meta (list :parcera.core/failure (cons rule children))
             (assoc-in metadata [:parcera.core/start :message] message)))


(defn failure
  "Checks that `rule` conforms to additional rules which are too difficult
  to represent with pure Antlr4 syntax"
  [rule children metadata]
  (case rule
    (:symbol :macro_keyword)
    (when (string? (first children))
      (let [rule-spec (keyword "parcera.spec" (name rule))]
        (when (not (s/valid? rule-spec (first children)))
          (report rule children metadata
                  (s/explain-str rule-spec (first children))))))
    nil))


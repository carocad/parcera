(ns parcera.spec
  "Specifications for the **content** of some Antlr parser rules
  which would otherwise be too difficult to express"
  (:require [clojure.spec.alpha :as s]))

;; a name can contain a maximum of 1 /
(def qualified-name #?(:clj  #"^([^\s\/]+\/)?(\/|[^\s\/]+)$"
                       ;; for some reason cljs doesnt accept escaping the / characters
                       :cljs #"^([^\s/]+/)?(/|[^\s/]+)$"))


(defn- qualified-name? [text] (re-find qualified-name text))

;; a symbol cannot start with a number
(defn- symbol-number-start? [text] (re-find #"^[+-]?\d+" text))

(defn- keep-forms [coll] (remove (comp #{:whitespace :discard} first) coll))


(s/def ::symbol (s/and string? qualified-name? (complement symbol-number-start?)))

(s/def ::simple_keyword (s/and string? qualified-name?))

(s/def ::macro_keyword (s/and string? qualified-name? (complement #{"/"})))

(s/def ::map (s/and (s/conformer keep-forms) #(even? (count %))))

(s/def ::set (s/and (s/conformer keep-forms) #(= (count %) (count (distinct %)))))


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
    (:symbol :simple_keyword :macro_keyword)
    (when (string? (first children))
      (let [rule-spec (keyword "parcera.spec" (name rule))]
        (when (not (s/valid? rule-spec (first children)))
          (report rule children metadata
                  (s/explain-str rule-spec (first children))))))

    (:map :set)
    (let [rule-spec (keyword "parcera.spec" (name rule))]
      (when (not (s/valid? rule-spec children))
        (report rule children metadata
                (s/explain-str rule-spec children))))

    nil))


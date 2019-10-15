(ns parcera.terminals)

;; Clojure's reader is quite permissive so we follow the motto
;; "if it is not forbidden, it is allowed"
(def not-allowed "\\s\\(\\)\\[\\]{}\"@~\\^;`\\\\\\/,")
(def allowed-characters (str "[^" not-allowed "]*"))
(def not-number "(?![+-]?\\d+)")
(def symbol-end "(?=[\\s\"()\\[\\]{},]|$)")

(defn- name-pattern
  [restriction]
  (let [first-character (str "[^" restriction not-allowed "]")]
    (str "(" first-character allowed-characters "\\/)?"
         "(\\/|(" first-character allowed-characters "))"
         symbol-end)))

(def symbol-pattern (str not-number (name-pattern ":#\\'")))

(def double-suffix "(((\\.\\d*)?([eE][-+]?\\d+)?)M?)")
(def long-suffix "((0[xX]([\\dA-Fa-f]+)|0([0-7]+)|([1-9]\\d?)[rR]([\\d\\w]+)|0\\d+)?N?)")
(def ratio-suffix "(\\/(\\d+))")
(def number-pattern (str "[+-]?\\d+(" long-suffix "|" double-suffix "|" ratio-suffix ")(?![\\.\\/])")) ; todo: word boundary ?


; This is supposed to be the JavaScript friendly version of #'\P{M}\p{M}*+'
; mentioned here: https://www.regular-expressions.info/unicode.html
; It's cooked by this generator: http://kourge.net/projects/regexp-unicode-block
; ticking all 'Combining Diacritical Marks' boxes *))
; todo: repeated pattern could be simplified
(def unicode-char "([^\\u0300-\\u036F\\u1DC0-\\u1DFF\\u20D0-\\u20FF][\\u0300-\\u036F\\u1DC0-\\u1DFF\\u20D0-\\u20FF]*)")
(def named-char "(newline|return|space|tab|formfeed|backspace)")
(def unicode "(u[\\dD-Fd-f]{4})")
; todo: use word boundary to avoid lookahead
(def character-pattern (str "\\\\(" unicode-char "|" named-char "|" unicode ")(?!\\w+)"))


; : is not allowed as first keyword character
; todo: no need for negative lookahead of symbol
(def simple-keyword (str ":" (name-pattern ":")))
(def macro-keyword (str "::" (name-pattern ":")))


(def string-pattern "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"")
(def regex-pattern (str "#" string-pattern))

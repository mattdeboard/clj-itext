(ns render-pdf.utils
  (:import [com.itextpdf.text Document]))

(defn pdf? [d]
  (= (type d) Document))

(defmacro with-pdf [bindings & body]
  (#'clojure.core/assert-args
   (vector? bindings) "A vector for its binding"
   (even? (count bindings)) "an even number of forms in binding vector")
  (cond
   (= (count bindings) 0) `(do ~@body)
   (symbol? (bindings 0)) `(let ~(vector (bindings 0)
                                         `(doto ~(bindings 1) (.open)))
                             (try (with-pdf ~(subvec bindings 2) ~@body)
                                  (finally
                                   (. ~(bindings 0) close))))
   :else (throw (IllegalArgumentException.
                 "with-pdf only allows binding of Documents to Symbols"))))




(ns render-pdf.configparse
  (:use [clojure.data.zip.xml :only [attr attr= xml->]]
        [clojure.zip :only [xml-zip]]
        [clojure.xml]))

(defn configparse [attrib source]
  (let [conf (xml-zip (parse (clojure.java.io/file source "App.config")))]
    (first (xml-> conf :appSettings :add (attr= :key attrib)
                  (attr :value)))))

(defn timeout [source]
  (configparse "ghostscriptProcessTimeout" source))


(ns render-pdf.pdftoc
  (:use [clojure.data.json :only (read-json json-str)])
  (:import [com.itextpdf.text.pdf PdfPageLabels PdfReader SimpleBookmark]))

(defn pagecount
  "Return a vector of hashmaps suitable for transformation to JSON."
  [reader]
  (if-let [labels (PdfPageLabels/getPageLabels reader)]
    (map-indexed #(identity {:label % :pagenum %2}) labels)
    (map #(identity {:label % :pagenum (dec %)})
         (range (. reader getNumberOfPages)))))

(defn chapters
  "Return a Java Array of bookmark data."
  [reader]
  (SimpleBookmark/getBookmark reader))

(defn write-out
  "Write chapter and page data to their respective files in the `dest'
 directory."
  [in dest]
  (let [reader (new PdfReader in)
        d "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Bookmark></Bookmark>"
        jsonout (clojure.java.io/file dest "pagedata.json")
        xmlout (clojure.java.io/file dest "chapterdata.xml")]
    (with-open [w (clojure.java.io/writer xmlout)]
      (if-let [c (chapters reader)]
        (SimpleBookmark/exportToXML c w "UTF-8" false)
        (.write w d)))
    (with-open [w (clojure.java.io/writer jsonout)]
      (.write w (json-str (pagecount reader))))))


(ns render-pdf.core
  (:use [clojure.string :only (split join)])
  (:import [com.itextpdf.text Document]
           [com.itextpdf.text.pdf PdfCopy PdfReader PdfWriter]))


(defn- copy-page
  "Creates a PdfCopy instance and, after opening the associated document,
returns it.

This is icky and gross but an unfortunate side effect of the really goofy
iText API.
"
  [^Document document outstream]
  (let [pdfcopy (new PdfCopy document outstream)]
    (. document open)
    pdfcopy))

(defn- output-path
  "Return a `File' instance that represents a new file."
  [path filename ext pagenum]
  (let [fn (str filename "-" pagenum "." ext)]
    (join "/" [path fn])))

(defn- write-pdf [pagenum ^PdfReader reader writer]
  (let [page (. writer getImportedPage reader pagenum)]
    (. writer addPage page)
    (. writer close)))
        
(defn burst
  "'Burst' a single PDF with n pages into n separate PDF files."
  [^PdfReader reader]
  (let [doc (new Document (. reader getPageSizeWithRotation 1))
        spl (split (:filename (meta reader)) #"(\.|/)")
        ct (count spl)
        [path [filename ext]] (split-at (- ct 2) spl)
        pages (range 1 (inc (:pagecount (meta reader))))
        outpath (partial output-path (join "/" path) filename ext)]
    (for [pn pages]
      (with-open [os (clojure.java.io/output-stream (outpath pn))]
        (->> os (copy-page doc) (write-pdf pn reader))))))

(defmacro proxy-meta
  "This macro emits a function that can then be called with the appropriate
args to proxy a given class, extending it to implement the `IObj' interface."
  [class]
  `(fn make#
     [infile# & [moremeta#]]
     (let [metadata# (merge {:filename infile#} moremeta#)]
       (proxy [~class clojure.lang.IObj] [infile#]
         (withMeta [newmeta#] (make# infile# newmeta#))
         (meta [] metadata#)))))

(defn pdf<-
  "Returns PdfReader instance. The returned instance has metadata attached
to access various data about the PDF, such as the filename of the PDF, the
number of pages, and so forth."
  [infile]
  (let [rdr ((proxy-meta PdfReader) infile)]
    (vary-meta rdr assoc :pagecount (. rdr getNumberOfPages))))

(defn ->pdf
  "Returns a PdfWriter instance."
  [outfile]
  (with-open [w (clojure.java.io/output-stream outfile)]
    (let [doc (doto (new Document) (.open))
          wrtr (PdfWriter/getInstance doc w)]
      (. wrtr open)
      wrtr)))

;; (macroexpand-1
;;  `(let [w (->pdf "resources/foo1.pdf")]
;;     (. w getDirectContent)
;;     (. w close)))

;; (let [[d w] (->pdf "resources/foo1.pdf")]
;;   (. w getDirectContent)
;;   (. d isOpen))
;; (with-pdf-open [w (->pdf "resources/foo1.pdf")]
;;   (. w getDirectContent))

(ns render-pdf.core
  (:use [clojure.string :only (split join)])
  (:import [com.itextpdf.text Document PageSize Paragraph]
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
  [^String infile]
  (let [rdr ((proxy-meta PdfReader) infile)]
    (vary-meta rdr assoc :pagecount (. rdr getNumberOfPages))))

(defmacro ->pdf
  "Evaluates `body' as an operation against either the Document or
PdfWriter instance.

Inputs:
  :entity - Valid values for this are:
            :writer
            :document (default)
  :body     The expression to evaluate.
"
  [^String outfile & {:keys [entity body]
                      :or {entity :document}}]
  `(let [document# (Document. PageSize/A4 50 50 50 50)]
     (with-open [pw# (PdfWriter/getInstance
                      document#
                      (clojure.java.io/output-stream ~outfile))
                 doc# (doto document# (.open))]
       (if (= ~entity :writer)
         (doto pw# ~@body)
         (doto doc# ~@body)))))


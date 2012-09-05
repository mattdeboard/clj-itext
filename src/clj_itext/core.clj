(ns clj-itext.core
  (:use [clojure.string :only (split join)])
  (:import [com.itextpdf.text Document PageSize Paragraph]
           [com.itextpdf.text.pdf PdfCopy PdfReader PdfWriter SimpleBookmark]))

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
        (->> os (copy-page doc) (write-pdf pn reader))
        (outpath pn)
        ))))

(defmacro proxy-meta
  "This macro emits a function that can then be called with the appropriate
args to proxy a given class, extending it to implement the `IObj' interface.

The reason a macro is used instead of a function is because the first arg to
`(proxy)' is a collection, the first argument `(proxy)' is itself a macro. In
practical terms, this means that the args for `(proxy)' are evaluated at compile
 time, so they must be set then.
"
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
  (let [rdr ((proxy-meta PdfReader) infile)
        bm (SimpleBookmark/getBookmark rdr)
        ct (. rdr getNumberOfPages)]
    (vary-meta rdr assoc :pagecount ct :bookmarks bm)))


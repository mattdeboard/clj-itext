(ns render-pdf.core
  (:use [clojure.string :only (split join)])
  (:import [com.itextpdf.text Document]
           [com.itextpdf.text.pdf PdfCopy PdfReader]))

(defn proxy-meta
  "Returns an anonymous class instance, constructed with the file on the path
indicated by `infile', annotated with metadata indicating the filename used to
construct the instance. Optional `moremeta' is a single map containing any
additional metadata to add to the instance."
  [infile & moremeta]
  (let [metadata (merge {:filename infile} (first moremeta))]
    (proxy [PdfReader clojure.lang.IObj] [infile]
      (withMeta [new-meta] (merge metadata new-meta))
      (meta [] metadata))))

(defn with-proxy-meta
  "Returns a new copy of `obj' with metadata updated as indicated by
`metadata'.

Usage: `(with-proxy-meta (pdf<- path-to-pdf) {:foo 100})'
"
  [obj metadata]
  (let [key :filename
        new-meta (merge (meta obj) metadata)]
    (proxy-meta (key new-meta) (dissoc new-meta key))))

(defn- copy-page
  "Creates a PdfCopy instance and, after opening the associated document,
returns it.

This is icky and gross but an unfortunate side effect of the really goofy
iText API.
"
  [document outstream]
  (let [pdfcopy (new PdfCopy document outstream)]
    (. document open)
    pdfcopy))

(defn- output-path
  "Return a `File' instance that represents a new file."
  [path filename ext pagenum]
  (let [fn (str filename "-" pagenum "." ext)]
    (join "/" [path fn])))

(defn- write-pdf [pagenum reader writer]
  (let [page (. writer getImportedPage reader pagenum)]
    (. writer addPage page)
    (. writer close)))
        
(defn burst
  "'Burst' a single PDF with n pages into n separate PDF files."
  [reader]
  (let [doc (new Document (. reader getPageSizeWithRotation 1))
        spl (split (:filename (meta reader)) #"(\.|/)")
        ct (count spl)
        [path [filename ext]] (split-at (- ct 2) spl)
        pages (range 1 (inc (:pagecount (meta reader))))
        outpath (partial output-path (join "/" path) filename ext)]
    (for [pn pages]
      (with-open [os (clojure.java.io/output-stream (outpath pn))]
        (->> os (copy-page doc) (write-pdf pn reader))))
    (str "Processed " (:pagecount (meta reader)) " PDF pages.")))

(defn pdf<-
  "Returns PdfReader instance. The returned instance has metadata attached
to access various data about the PDF, such as the filename of the PDF, the
number of pages, and so forth."
  [infile]
  (let [rdr (proxy-meta infile)
        pages (. rdr getNumberOfPages)]
    (with-proxy-meta rdr {:pagecount pages})))




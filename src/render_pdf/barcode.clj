(ns render-pdf.barcode
  (:use [clojure.java.shell :only [sh]]
        render-pdf.configparse
        render-pdf.utils
        clojure.java.io)
  (:import [com.itextpdf.text BaseColor Document]
           [com.itextpdf.text.pdf Barcode128 PdfCopy PdfDocument PdfReader PdfStamper
            PdfWriter]))

(def ^:dynamic contentpath "resources")

(defn convert
  "Execute ImageMagick's 'convert' program in a shell subprocess."
  [source k]
  (sh "convert" "-type" "palettematte" (str k "_.png") (str k ".png")
      :dir source))

(defn make-pngs
  "Executes the 'convert' fn asynchronously and in parallel, returning a seq
of agent names that can be deref'd later."
  [source first-page last-page]
  (let [page-range (range first-page (inc last-page))
        agents (doall (map #(agent (str %)) page-range))]
    ;; At the time `send-off' is called, the state of the agent will be a
    ;; string that matches one of the pages in the `source' directory. This
    ;; value will be passed to `(partial convert source)' as the final arg.
    (doseq [a agents] (send-off a (partial convert source)))
    ;; Wait for some specified amount of time (set in `App.config', see
    ;; render-pdf.configparse for the attribute name) for the processes to
    ;; complete. 
    (apply await-for (Integer/parseInt (timeout source)) agents)
    ;; Return the agents' values after calling `(make-pngs)'. Should be a
    ;; vector of maps.
    (doall (map #(deref %) agents))))

(defn pdf-reader [i]
  (agent (new PdfReader (str "resources/pages/" i ".pdf"))))

(defn stitch-pdf []
  (let [pages (range 1 5)
        stream (new java.io.ByteArrayOutputStream)
        doc (new Document)
        pdfcopy (new PdfCopy doc stream)
        agents (doall (map pdf-reader pages))]
    (.open doc)
    (doseq [a agents] (send-off a (fn [x] (. pdfcopy (getImportedPage x 1)))))
    (apply await-for 30000 agents)
    (let [pages (doall (map #(deref %) agents))]
      (doall (map #(. pdfcopy (addPage %)) pages)))
    (.close doc)
    (.toByteArray stream)))

;; (defn get-barcode [i]
;;   (with-open [w (clojure.java.io/output-stream (str i "b_.pdf"))]
;;     (let [doc (new Document)
;;           pdfw (PdfWriter/getInstance doc w)]
;;       (.open doc)
;;       (let [barcode (new Barcode128)
;;             fg-color (new BaseColor 255 255 255 60)]
;;         (doto doc (.add (. barcode (createImageWithBarcode
;;                                     (. pdfw getDirectContent) fg-color nil)))))
;;       (println (. doc getPageSize))
;;       (.close doc)
;;       (.close pdfw)
;;       pdfw)))

(defn get-barcode [i]
  (with-open [w (clojure.java.io/output-stream (str i "b_.pdf"))]
    (let [doc (new Document)
          pdfw (PdfWriter/getInstance doc w)]
      (.open doc)
      (let [barcode (new Barcode128)
            fg-color (new BaseColor 255 255 255)]
        (. barcode (createImageWithBarcode
                    (. pdfw getDirectContent) fg-color nil))))))

(def bi (get-barcode 3))
(. bi getHeight)
(def pdfw (get-barcode 3))
(.close pdfw)


(def pdfr (new PdfReader (stitch-pdf)))

(let [mediabox (. pdfr getPageSize 3)
      bcp 10
      bch (/ (- (- (. mediabox getHeight) 8) 20) 3)
      x (- (+ (. mediabox getLeft) (. mediabox getWidth)) 33.656)
      y (+ (+ (* 3 (+ bcp bch)) 4) (. mediabox getBottom))]
  (doto bi (.setAbsolutePosition x y)))

(println (. (. pdfr getPageSize 3) getBottom))
(with-open [w (output-stream "resources/sample.pdf")]
  (def stamper (new PdfStamper pdfr w)))

(. (. stamper getReader) getNumberOfPages)
(doto (. stamper getUnderContent 1) (.addImage bi))

# clj-itext

The iText API is extremely painful. This is an effort to make it less so by wrapping the Java bindings in more friendly interfaces.

Features:
 - Simplified PdfReader instance generation
 - ...more coming soon...

## Example Usage

``(pdf<- "/path/to/sample.pdf")``

returns a ``PdfReader`` instance with the following metadata:
 - ``:bookmarks``
 - ``:pagecount``
 - ``:filename``

Using metadata removes the need to write extra code in your application to get this information. It can be accessed via ``meta``, e.g. ``(meta (pdf<- "/path/to/sample.pdf"))``.

Also included is the ``burst`` fn:

``(burst (pdf<- "/path/to/sample.pdf"))``

This fn takes a PDF with *n* pages and explodes it into *n* PDF files, one for each page. Its return value is a list of the new filenames. (By default, for a file named ``sample.pdf``, the new filenames will be ``sample-n.pdf``, with ``n`` representing the index of that page in the master document.)

## To Do

- Smooth out ``PdfWriter`` instantiation & interaction (this is the hairiest wart on the iText API, IMO)
- Other human-friendly improvements to the API as they come up.

## License

Copyright © 2012 Matt DeBoard

Distributed under the Eclipse Public License, the same as Clojure.

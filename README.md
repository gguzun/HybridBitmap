# HybridBitmap
Operates compressed and non-compressed bit-vectors
<p>
 The Hybrid supports operation of EWAH compressed bit-vectors and verbatim bit-vectors. Proved to be efficient for high-density bitmaps, 
 however can be used in all cases, as it imposes a very small overhead.  
 </p>


<p>
 For more details, see the following papers:
 </p>
 
  <p>
 The Hybrid optimization paper:
 </p>
 
 <ul>
 <li>Gheorghi Guzun, and Guadalupe Canahuate. "Hybrid Query Optimization for Hard-to-Compress Bit-vectors". VLDBJ, In press - 2015</li>
 </ul>
 
   <p>
 The EWAH paper:
 </p>
 
 <ul>
 <li>Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves
 word-aligned bitmap indexes. Data & Knowledge Engineering 69 (1), pages
 3-28, 2010. http://arxiv.org/abs/0901.3751</li>
 </ul>
 
 <p>
 A 32-bit version of the compressed format was described by Wu et al. and
 named WBC:
 </p>
 
 <ul>
 <li>K. Wu, E. J. Otoo, A. Shoshani, H. Nordberg, Notes on design and
 implementation of compressed bit vectors, Tech. Rep. LBNL/PUB-3161,
 Lawrence Berkeley National Laboratory, available from http://crd.lbl.
 gov/~kewu/ps/PUB-3161.html (2001).</li>
 </ul>
 
 <p>
 Probably, the best prior art is the Oracle bitmap compression scheme
 (BBC):
 </p>
 <ul>
 <li>G. Antoshenkov, Byte-Aligned Bitmap Compression, DCC'95, 1995.</li>
 </ul>


package hybridewah;


/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc. and Veronika Zenz
 * Modified by Gheorghi Guzun to support operation of compressed and verbatim bitmaps.
 * Licensed under APL 2.0.
 */

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import topK.topPrefEWAH;


/**
 * <p>
 * This implements the patent-free(1) EWAH scheme. Roughly speaking, it is a
 * 64-bit variant of the BBC compression scheme used by Oracle for its bitmap
 * indexes.
 * </p>
 * 
 * <p>
 * The objective of this compression type is to provide some compression, while
 * reducing as much as possible the CPU cycle usage.
 * </p>
 * 
 * 
 * <p>
 * This implementation being 64-bit, it assumes a 64-bit CPU together with a
 * 64-bit Java Virtual Machine. This same code on a 32-bit machine may not be as
 * fast.
 * <p>
 * 
 *  
 *
 * 
 * <p>
 * The Hybrid supports operation of compressed and verbatim bitmaps. Proved to be efficient for high-density bitmaps, 
 * however can be used in all cases, as it imposes a very small overhead.  
 * </p>
 * 
 * 
 * @see com.googlecode.javaewah32.EWAHCompressedBitmap32
 * 
 *      <p>
 *      For more details, see the following papers:
 *      </p>
 * 
 *      <ul>
 *      <li>Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves
 *      word-aligned bitmap indexes. Data & Knowledge Engineering 69 (1), pages
 *      3-28, 2010. http://arxiv.org/abs/0901.3751</li>
 *      </ul>
 *      
 *       <p>
 *      The Hybrid optimization paper:
 *      </p>
 * 
 *      <ul>
 *      <li>Guzun, Gheorghi, and Guadalupe Canahuate. "Hybrid Query Optimization for Hard-to-Compress Bit-vectors". VLDBJ, 2015</li>
 *      </ul>
 * 
 *      <p>
 *      A 32-bit version of the compressed format was described by Wu et al. and
 *      named WBC:
 *      </p>
 * 
 *      <ul>
 *      <li>K. Wu, E. J. Otoo, A. Shoshani, H. Nordberg, Notes on design and
 *      implementation of compressed bit vectors, Tech. Rep. LBNL/PUB-3161,
 *      Lawrence Berkeley National Laboratory, available from http://crd.lbl.
 *      gov/~kewu/ps/PUB-3161.html (2001).</li>
 *      </ul>
 * 
 *      <p>
 *      Probably, the best prior art is the Oracle bitmap compression scheme
 *      (BBC):
 *      </p>
 *      <ul>
 *      <li>G. Antoshenkov, Byte-Aligned Bitmap Compression, DCC'95, 1995.</li>
 *      </ul>
 * 
 *      <p>
 *      1- The authors do not know of any patent infringed by the following
 *      implementation. However, similar schemes, like WAH are covered by
 *      patents.
 *      </p>
 * 
 * @since 0.1.0
 */
public final class HybridBitmap implements Cloneable, Serializable,
  Iterable<Integer>, BitmapStorage, LogicalElement<HybridBitmap> {
//	private static final long serialVersionUID =  -3518951799105739620L;
	
	private String name = "";
  /**
   * Creates an empty bitmap (no bit set to true).
   */
  public HybridBitmap() {
    this.buffer = new long[defaultbuffersize];
    this.rlw = new RunningLengthWord(this.buffer, 0);
  }

  /**
   * Sets explicitly the buffer size (in 64-bit words). The initial memory usage
   * will be "buffersize * 64". For large poorly compressible bitmaps, using
   * large values may improve performance.
   * 
   * @param buffersize
   *          number of 64-bit words reserved when the object is created)
   */
  public HybridBitmap(final int buffersize) {
    this.buffer = new long[buffersize];
    this.rlw = new RunningLengthWord(this.buffer, 0);
  }

	public HybridBitmap(final boolean verbatim) {
		this.verbatim = verbatim;
		this.buffer = new long[defaultbuffersize];
		this.rlw = new RunningLengthWord(this.buffer, 0);
		if (verbatim)
			this.actualsizeinwords = 0;
	}
	
	public HybridBitmap(final boolean verbatim, final int size) {
		this.verbatim = verbatim;
		this.buffer = new long[size];
		this.rlw = new RunningLengthWord(this.buffer, 0);
		if (verbatim)
			this.actualsizeinwords = 0;
	}

  /**
   * Adding words directly to the bitmap (for expert use).
   * 
   * This is normally how you add data to the array. So you add bits in streams
   * of 8*8 bits.
   * 
   * @param newdata
   *          the word
   */
  public void add(final long newdata) {
    add(newdata, wordinbits);
  }

  /**
   * Adding words directly to the bitmap (for expert use).
   * 
   * @param newdata
   *          the word
   * @param bitsthatmatter
   *          the number of significant bits (by default it should be 64)
   */
  public void add(final long newdata, final int bitsthatmatter) {
    this.sizeinbits += bitsthatmatter;
    if (newdata == 0) {
      addEmptyWord(false);
    } else if (newdata == ~0l) {
      addEmptyWord(true);
    } else {
      addLiteralWord(newdata);
    }
  }
  
	public void addVerbatim(long newdata) {
		if (this.actualsizeinwords == this.buffer.length) {
			final long oldbuffer[] = this.buffer;
			this.buffer = new long[oldbuffer.length * 2];
			System.arraycopy(oldbuffer, 0, this.buffer, 0, oldbuffer.length);
			// this.rlw.array = this.buffer;
		}
		this.buffer[this.actualsizeinwords++] = newdata;
		this.sizeinbits+=wordinbits;
	}

  @Override
	public void setColName(String colname) {
            name = colname;
        }
  
  
 	public String getColName() {
             return name;
         }

  /**
   * For internal use.
   * 
   * @param v
   *          the boolean value
   */
  private void addEmptyWord(final boolean v) {
    final boolean noliteralword = (this.rlw.getNumberOfLiteralWords() == 0);
    final long runlen = this.rlw.getRunningLength();
    if ((noliteralword) && (runlen == 0)) {
      this.rlw.setRunningBit(v);
    }
    if ((noliteralword) && (this.rlw.getRunningBit() == v)
      && (runlen < RunningLengthWord.largestrunninglengthcount)) {
      this.rlw.setRunningLength(runlen + 1);
      return;
    }
    push_back(0);
    this.rlw.position = this.actualsizeinwords - 1;
    this.rlw.setRunningBit(v);
    this.rlw.setRunningLength(1);
    return;
  }

  /**
   * For internal use.
   * 
   * @param newdata
   *          the literal word
   */
  private void addLiteralWord(final long newdata) {
    final int numbersofar = this.rlw.getNumberOfLiteralWords();
    if (numbersofar >= RunningLengthWord.largestliteralcount) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      this.rlw.setNumberOfLiteralWords(1);
      push_back(newdata);
    }
    this.rlw.setNumberOfLiteralWords(numbersofar + 1);
    push_back(newdata);
  }

  /**
   * if you have several literal words to copy over, this might be faster.
   * 
   * 
   * @param data
   *          the literal words
   * @param start
   *          the starting point in the array
   * @param number
   *          the number of literal words to add
   */
  public void addStreamOfLiteralWords(final long[] data, final int start,
    final int number) {
    if (number == 0)
      return;
    final int NumberOfLiteralWords = this.rlw.getNumberOfLiteralWords();
    final int whatwecanadd = number < RunningLengthWord.largestliteralcount
      - NumberOfLiteralWords ? number : RunningLengthWord.largestliteralcount
      - NumberOfLiteralWords;
    this.rlw.setNumberOfLiteralWords(NumberOfLiteralWords + whatwecanadd);
    final int leftovernumber = number - whatwecanadd;
    push_back(data, start, whatwecanadd);
    this.sizeinbits += whatwecanadd * wordinbits;
    if (leftovernumber > 0) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      addStreamOfLiteralWords(data, start + whatwecanadd, leftovernumber);
    }
    return;
  }

  /**
   * For experts: You want to add many zeroes or ones? This is the method you
   * use.
   * 
   * @param v
   *          the boolean value
   * @param number
   *          the number
   */
  public void addStreamOfEmptyWords(final boolean v, long number) {
    if (number == 0)
      return;
    this.sizeinbits += number * wordinbits;
    if ((this.rlw.getRunningBit() != v) && (this.rlw.size() == 0)) {
      this.rlw.setRunningBit(v);
    } else if ((this.rlw.getNumberOfLiteralWords() != 0)
      || (this.rlw.getRunningBit() != v)) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      if (v)
        this.rlw.setRunningBit(v);
    }
    final long runlen = this.rlw.getRunningLength();
    final long whatwecanadd = number < RunningLengthWord.largestrunninglengthcount
      - runlen ? number : RunningLengthWord.largestrunninglengthcount - runlen;
    this.rlw.setRunningLength(runlen + whatwecanadd);
    number -= whatwecanadd;
    while (number >= RunningLengthWord.largestrunninglengthcount) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      if (v)
        this.rlw.setRunningBit(v);
      this.rlw.setRunningLength(RunningLengthWord.largestrunninglengthcount);
      number -= RunningLengthWord.largestrunninglengthcount;
    }
    if (number > 0) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      if (v)
        this.rlw.setRunningBit(v);
      this.rlw.setRunningLength(number);
    }
  }

  /**
   * Same as addStreamOfLiteralWords, but the words are negated.
   * 
   * @param data
   *          the literal words
   * @param start
   *          the starting point in the array
   * @param number
   *          the number of literal words to add
   */
  public void addStreamOfNegatedLiteralWords(final long[] data, final int start,
    final int number) {
    if (number == 0)
      return;
    final int NumberOfLiteralWords = this.rlw.getNumberOfLiteralWords();
    final int whatwecanadd = number < RunningLengthWord.largestliteralcount
      - NumberOfLiteralWords ? number : RunningLengthWord.largestliteralcount
      - NumberOfLiteralWords;
    this.rlw.setNumberOfLiteralWords(NumberOfLiteralWords + whatwecanadd);
    final int leftovernumber = number - whatwecanadd;
    negative_push_back(data, start, whatwecanadd);
    this.sizeinbits += whatwecanadd * wordinbits;
    if (leftovernumber > 0) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      addStreamOfLiteralWords(data, start + whatwecanadd, leftovernumber);
    }
    return;
  }

  /**
   * Returns a new compressed bitmap containing the bitwise AND values of the
   * current bitmap with some other bitmap.
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @since 0.4.3
   * @param a
   *          the other bitmap
   * @return the EWAH compressed bitmap
   */
  public HybridBitmap and(final HybridBitmap a) {
	 	  
    final HybridBitmap container = new HybridBitmap();
    //double expDens = this.setbits/(double)this.sizeinbits*a.setbits/(double)a.sizeinbits;
   // double expDens = this.density*a.density;
   // double expDens = (this.setbits*a.setbits)/((this.sizeinbits*a.sizeinbits)); // expected bit density of the result
    //container.setbits= (expDens*this.sizeinbits);
    container.density=this.density*a.density;   
		if (this.verbatim && a.verbatim) {				
			//if(container.density<andThreshold){
			if(Math.min(this.density, a.density)<andThreshold){
				if(container.density==0){
					container.verbatim=false;
					container.setSizeInBits(Math.max(this.sizeinbits, a.sizeinbits),false);
				}else{					
				this.andVerbatimCompress(a, container);		
				}		
			}else{
				this.andVerbatim(a, container);
			}								
		} else if(this.verbatim || a.verbatim) {
			if(container.density==0){
				container.verbatim=false;
				container.setSizeInBits(Math.max(this.sizeinbits, a.sizeinbits),false);
			}else{
			this.andHybridCompress(a, container);
			container.setSizeInBits(Math.max(this.sizeinbits, a.sizeinbits));
			}			
		}else{
			if(container.density==0){
				container.verbatim=false;
				container.setSizeInBits(Math.max(this.sizeinbits, a.sizeinbits),false);
			}else{
			container.reserve(this.actualsizeinwords > a.actualsizeinwords ? this.actualsizeinwords : a.actualsizeinwords);
			this.andToContainer(a, container);
			}
		}
		container.age = Math.max(this.age, a.age)+1;
		if(container.age>20){	
			container.density=container.cardinality();
		container.age=0;
		}
		return container;		
	}
  
 
	public HybridBitmap or(final HybridBitmap a) {
		final HybridBitmap container = new HybridBitmap();
		//double expDens = (this.setbits+a.setbits)/(double)(this.sizeinbits)-(this.setbits/(double)this.sizeinbits*a.setbits/(double)a.sizeinbits);
		 container.density= (this.density+a.density)-(this.density*a.density);
		//    container.sizeinbits=this.sizeinbits;
		if (this.verbatim && a.verbatim) {
			if(container.density>(1-orThreshold)){
			//if(Math.max(this.density, a.density)>){	
				orVerbatimCompress(a,container);
			}else
			orVerbatim(a, container);
		}
		
		else if(this.verbatim || a.verbatim){
			if(container.density>(1-orThreshold)){
				orHybridCompress(a,container);
			}else{
			this.orHybrid(a, container);
			}
		}else{
			//if(container.density>orThreshold){
			if(Math.max(this.density, a.density)>orThreshold){
				this.orToContainerDecompress(a, container);
			}else{			
			container.reserve(this.actualsizeinwords + a.actualsizeinwords);
			orToContainer(a, container);}
		}
		container.age = Math.max(this.age, a.age)+1;
		if(container.age>20){	
			container.density=container.cardinality();
		container.age=0;
		}
		return container;
	}

	public HybridBitmap xor(final HybridBitmap a) {
		final HybridBitmap container = new HybridBitmap();
		//double expDens = (this.setbits+a.setbits)/(double)(this.sizeinbits)-(this.setbits*a.setbits)/(double)(this.sizeinbits*a.sizeinbits);
		//double expDens = this.setbits/(double)this.sizeinbits*(a.sizeinbits-a.setbits)/(double)a.sizeinbits+a.setbits/(double)a.sizeinbits*(this.sizeinbits-this.setbits)/(double)this.sizeinbits;
		container.density=this.density*(1-a.density)+a.density*(1-this.density);
		//    container.sizeinbits=this.sizeinbits;
		if (this.verbatim && a.verbatim) {
			xorVerbatim(a, container);
		}else if(this.verbatim || a.verbatim){
			this.xorHybrid(a, container);
		}else{
			//if(container.density>orThreshold){
			if(Math.max(this.density, a.density)>orThreshold){
				this.xorToContainerDecompress(a, container);
			}else{			
			container.reserve(this.actualsizeinwords + a.actualsizeinwords);
			xorToContainer(a, container);}
		}
		container.age = Math.max(this.age, a.age)+1;
		if(container.age>20){	
			container.density=container.cardinality();
		container.age=0;
		}
		return container;
	}

	public HybridBitmap andNot(final HybridBitmap a) {
	    final HybridBitmap container = new HybridBitmap();
	    //double expDens = this.setbits/(double)this.sizeinbits*(a.sizeinbits-a.setbits)/(double)a.sizeinbits;
	    container.density=this.density*(1-a.density);
	    //container.sizeinbits=this.sizeinbits;
	    if (this.verbatim && a.verbatim) {						
				//if(container.density<andThreshold){
	    	if(Math.min(this.density, (1-a.density))<andThreshold){
					if(this.density==0){
						container.verbatim=false;
						container.addStreamOfEmptyWords(false, (this.sizeinbits>>>6));
						container.density=0;
					}else{					
					this.andNotVerbatimCompress(a, container);	}				
									
				}else{
					this.andNotVerbatim(a, container);
					
				}				
					
			} else if(this.verbatim || a.verbatim) {
				if(this.density==0){
					container.verbatim=false;
					container.addStreamOfEmptyWords(false, (this.sizeinbits>>>6));
				}else{
				this.andNotHybridCompress(a, container);}
				
			}else{
				if(this.density==0){
					container.verbatim=false;
					container.addStreamOfEmptyWords(false, (this.sizeinbits>>>6));
				}else{
				container.reserve(this.actualsizeinwords > a.actualsizeinwords ? this.actualsizeinwords : a.actualsizeinwords);
				this.andNotToContainer(a, container);}
			}
	    container.age = Math.max(this.age, a.age)+1;
		if(container.age>20){	
			container.density=container.cardinality();
		container.age=0;
		}
			return container;
		}
	
	 public HybridBitmap andV(final HybridBitmap a) {
	 	  
		    final HybridBitmap container = new HybridBitmap();
		    this.andVerbatim(a, container);	
		    return container;
	  }
	 
	 public HybridBitmap orV(final HybridBitmap a) {
	 	  
		    final HybridBitmap container = new HybridBitmap();
		    this.orVerbatim(a, container);	
		    return container;
	  }
	 
	 public HybridBitmap xorV(final HybridBitmap a) {
	 	  
		    final HybridBitmap container = new HybridBitmap();
		    this.xorVerbatim(a, container);	
		    return container;
	  }
	 
	 public HybridBitmap andNotV(final HybridBitmap a) {
	 	  
		    final HybridBitmap container = new HybridBitmap();
		    this.andNotVerbatim(a, container);	
		    return container;
	  }
	 
	 public HybridBitmap andC(final HybridBitmap a) {
	 	  
		    final HybridBitmap container = new HybridBitmap();
		    container.reserve(this.actualsizeinwords > a.actualsizeinwords ? this.actualsizeinwords : a.actualsizeinwords);
		    this.andToContainer(a, container);	
		    return container;
	  }
	 
	 public HybridBitmap orC(final HybridBitmap a) {
	 	  
		    final HybridBitmap container = new HybridBitmap();
		    container.reserve(this.actualsizeinwords + a.actualsizeinwords);
		    this.orToContainer(a, container);	
		    return container;
	  }
	 
	 public HybridBitmap xorC(final HybridBitmap a) {
	 	  
		    final HybridBitmap container = new HybridBitmap();
		    container.reserve(this.actualsizeinwords + a.actualsizeinwords);
		    this.xorToContainer(a, container);	
		    return container;
	  }
	 
	 public HybridBitmap andNotC(final HybridBitmap a) {
	 	  
		    final HybridBitmap container = new HybridBitmap();
		    container.reserve(this.actualsizeinwords > a.actualsizeinwords ? this.actualsizeinwords : a.actualsizeinwords);
		    this.andNotToContainer(a, container);	
		    return container;
	  }
   /**
   * Computes new compressed bitmap containing the bitwise AND values of the
   * current bitmap with some other bitmap.
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @param container
   *          where we store the result
   */
  public void andToContainer(final HybridBitmap a, final BitmapStorage container) {
    final EWAHIterator i = a.getEWAHIterator();
    final EWAHIterator j = getEWAHIterator();
    final IteratingBufferedRunningLengthWord rlwi = new IteratingBufferedRunningLengthWord(i);
    final IteratingBufferedRunningLengthWord rlwj = new IteratingBufferedRunningLengthWord(j);
    while ((rlwi.size()>0) && (rlwj.size()>0)) {
      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
        final boolean i_is_prey = rlwi.getRunningLength() < rlwj
          .getRunningLength();
        final IteratingBufferedRunningLengthWord prey = i_is_prey ? rlwi : rlwj;
        final IteratingBufferedRunningLengthWord predator = i_is_prey ? rlwj
          : rlwi;
        if (predator.getRunningBit() == false) {
          container.addStreamOfEmptyWords(false, predator.getRunningLength());
          prey.discardFirstWords(predator.getRunningLength());
          predator.discardFirstWords(predator.getRunningLength());
        } else {
          final long index = prey.discharge(container, predator.getRunningLength()); 
          container.addStreamOfEmptyWords(false, predator.getRunningLength()
            - index);
          predator.discardFirstWords(predator.getRunningLength());
        }
      }
      final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(),
        rlwj.getNumberOfLiteralWords());
      if (nbre_literal > 0) {
        for (int k = 0; k < nbre_literal; ++k)
          container.add(rlwi.getLiteralWordAt(k) & rlwj.getLiteralWordAt(k));
        rlwi.discardFirstWords(nbre_literal);
        rlwj.discardFirstWords(nbre_literal);
      }
    }      
    final boolean i_remains = rlwi.size()>0;
    final IteratingBufferedRunningLengthWord remaining = i_remains ? rlwi : rlwj;
    remaining.dischargeAsEmpty(container);
    container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));

  }
  
  public void andToContainerDecompress(final HybridBitmap a, final HybridBitmap container) {
	  
	  container.reserve(((this.sizeinbits>>>6)+1));
	  container.verbatim=true;
	  container.actualsizeinwords=0;
	    final EWAHIterator i = a.getEWAHIterator();
	    final EWAHIterator j = getEWAHIterator();
	    final IteratingBufferedRunningLengthWord rlwi = new IteratingBufferedRunningLengthWord(i);
	    final IteratingBufferedRunningLengthWord rlwj = new IteratingBufferedRunningLengthWord(j);
	    while ((rlwi.size()>0) && (rlwj.size()>0)) {
	      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
	        final boolean i_is_prey = rlwi.getRunningLength() < rlwj
	          .getRunningLength();
	        final IteratingBufferedRunningLengthWord prey = i_is_prey ? rlwi : rlwj;
	        final IteratingBufferedRunningLengthWord predator = i_is_prey ? rlwj
	          : rlwi;
	        if (predator.getRunningBit() == false) {
	         // container.addStreamOfEmptyWords(false, predator.getRunningLength());
	          Arrays.fill(container.buffer, container.actualsizeinwords, (int) (container.actualsizeinwords+predator.getRunningLength()), 0);
	          container.actualsizeinwords+=predator.getRunningLength();
	          prey.discardFirstWords(predator.getRunningLength());
	          predator.discardFirstWords(predator.getRunningLength());
	        } else {
	        	
	        	 long index = prey.dischargeDecompressed(container, predator.getRunningLength());
		          Arrays.fill(container.buffer, container.actualsizeinwords, (int) (container.actualsizeinwords+predator.getRunningLength()-index), 0);
	          //final long index = prey.discharge(container, predator.getRunningLength()); 
	         // container.addStreamOfEmptyWords(false, predator.getRunningLength()	            - index);
		          container.actualsizeinwords+=predator.getRunningLength()-index;
		          predator.discardFirstWords(predator.getRunningLength());
	        }
	      }
	      final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(),
	        rlwj.getNumberOfLiteralWords());
	      if (nbre_literal > 0) {
	        for (int k = 0; k < nbre_literal; ++k){
	        	container.buffer[container.actualsizeinwords]=(rlwi.getLiteralWordAt(k) & rlwj.getLiteralWordAt(k));
		          container.actualsizeinwords++;
	          }
	        rlwi.discardFirstWords(nbre_literal);
	        rlwj.discardFirstWords(nbre_literal);
	      }
	    }      
	    container.sizeinbits=Math.max(sizeInBits(), a.sizeInBits());
	  }
  
  
  
	

	
	public void andVerbatimCompress(final HybridBitmap a, final HybridBitmap container) {
		container.reserve(this.actualsizeinwords);
		//container.verbatim = false;
		for (int i = 0; i < this.actualsizeinwords; i++) {
			container.add(this.buffer[i] & a.buffer[i]);
		}
		

	}

	public void andNotVerbatimCompress(final HybridBitmap a, final HybridBitmap container) {
		container.reserve(this.actualsizeinwords);
		container.verbatim = false;

		for (int i = 0; i < this.actualsizeinwords; i++) {
			container.add(this.buffer[i] & ~a.buffer[i]);
		}

	}

	public void orVerbatimCompress(final HybridBitmap a, final HybridBitmap container) {
		container.reserve(this.actualsizeinwords);
		container.verbatim = false;

		for (int i = 0; i < this.actualsizeinwords; i++) {
			container.add(this.buffer[i] | a.buffer[i]);
		}

	}

	public void xorVerbatimCompress(final HybridBitmap a, final HybridBitmap container) {
		container.reserve(this.actualsizeinwords);
		container.verbatim = false;

		for (int i = 0; i < this.actualsizeinwords; i++) {
			container.add(this.buffer[i] ^ a.buffer[i]);
		}

	}
	
	public void andVerbatim(final HybridBitmap a, final HybridBitmap container) {
		container.reserve(this.actualsizeinwords);
		container.verbatim = true;
		container.actualsizeinwords=0;
		

		for (int i = 0; i < this.actualsizeinwords; i++) {
			container.buffer[i] = this.buffer[i] & a.buffer[i];
			
		}

		container.actualsizeinwords = this.actualsizeinwords;
		//container.sizeinbits = this.actualsizeinwords <<6;
		container.sizeinbits = Math.max(this.sizeinbits, a.sizeinbits);

	}

	public void orVerbatim(final HybridBitmap a, final HybridBitmap container) {
		container.reserve(this.actualsizeinwords);
		container.verbatim = true;

		for (int i = 0; i < this.actualsizeinwords; i++) {
			container.buffer[i] = this.buffer[i] | a.buffer[i];
		}

		container.actualsizeinwords = this.actualsizeinwords;
		//container.sizeinbits = this.actualsizeinwords <<6;
		container.sizeinbits = Math.max(this.sizeinbits, a.sizeinbits);

	}
	/**
	 * Three input xor. For verbatim bitmaps only!
	 * @param a
	 * @param b
	 * @return
	 */
	public HybridBitmap xor( final HybridBitmap a, final HybridBitmap b) {
		HybridBitmap container = new HybridBitmap(true, this.actualsizeinwords);		

		for (int i = 0; i < this.actualsizeinwords; i++) {
			container.buffer[i] = this.buffer[i] ^ a.buffer[i] ^ b.buffer[i];
		}

		container.actualsizeinwords = this.actualsizeinwords;
		//container.sizeinbits = this.actualsizeinwords <<6;
		container.sizeinbits = Math.max(b.sizeinbits, Math.max(this.sizeinbits, a.sizeinbits));
		return container;

	}
	
	public HybridBitmap maj(final HybridBitmap a, final HybridBitmap b){
		HybridBitmap container = new HybridBitmap(true, this.actualsizeinwords);
		for (int i = 0; i < this.actualsizeinwords; i++) {
			container.buffer[i] = (this.buffer[i] & a.buffer[i]) | (a.buffer[i] & b.buffer[i])
		              | (this.buffer[i] & b.buffer[i]);
		}
		container.actualsizeinwords = this.actualsizeinwords;
		container.sizeinbits = Math.max(b.sizeinbits, Math.max(this.sizeinbits, a.sizeinbits));
		return container;
	}

	public void xorVerbatim(final HybridBitmap a, final HybridBitmap container) {
		container.reserve(this.actualsizeinwords);
		container.verbatim = true;

		for (int i = 0; i < this.actualsizeinwords; i++) {
			container.buffer[i] = this.buffer[i] ^ a.buffer[i];
		}

		container.actualsizeinwords = this.actualsizeinwords;
		//container.sizeinbits = this.actualsizeinwords <<6;
		container.sizeinbits = Math.max(this.sizeinbits, a.sizeinbits);

	}

	public void andNotVerbatim(final HybridBitmap a, final HybridBitmap container) {
		container.reserve(this.actualsizeinwords);
		container.verbatim = true;

		for (int i = 0; i < this.actualsizeinwords; i++) {
			container.buffer[i] = this.buffer[i] & (~a.buffer[i]);
		}

		container.actualsizeinwords = this.actualsizeinwords;
		//container.sizeinbits = this.actualsizeinwords <<6;
		container.sizeinbits = Math.max(this.sizeinbits, a.sizeinbits);

	}

	public void andHybridCompress(final HybridBitmap a, final HybridBitmap container) {
		int j = 0;
		int i=0;
		

		if (this.verbatim) { // this is verbatim
			container.reserve(a.actualsizeinwords);
			a.rlw = new RunningLengthWord(a.buffer, 0);
			
			while (i < this.actualsizeinwords) {
				if (a.rlw.getRunningBit()) { // fill of ones
					for (j = 0; j < a.rlw.getRunningLength(); j++) {
						
						container.add(this.buffer[i]);
						//container.setbits+=Long.bitCount(newdata);
						
						i++;
					}
				} else {
					container.addStreamOfEmptyWords(false, a.rlw.getRunningLength());
					i+=a.rlw.getRunningLength(); 
				}

				for( j=0; j<a.rlw.getNumberOfLiteralWords(); j++){
					
					container.add(this.buffer[i] & a.buffer[a.rlw.position + j + 1]);
					//container.setbits+=Long.bitCount(newdata);
					i++;
				}
				a.rlw.position += a.rlw.getNumberOfLiteralWords() + 1;

			}
		} else { // a is verbatim
			container.reserve(this.actualsizeinwords);
			this.rlw = new RunningLengthWord(this.buffer, 0);
			
			while (i < a.actualsizeinwords) {
				if (this.rlw.getRunningBit()) { // fill of ones
					for (j = 0; j < this.rlw.getRunningLength(); j++) {
						
						container.add(a.buffer[i]);
						//container.setbits+=Long.bitCount(newdata);
						i++;
					}
				} else {
					container.addStreamOfEmptyWords(false, this.rlw.getRunningLength());
					i+=this.rlw.getRunningLength(); 
				}

				for( j=0; j<this.rlw.getNumberOfLiteralWords(); j++){
				
					container.add(a.buffer[i] & this.buffer[this.rlw.position + j + 1]);					
					//container.setbits+=Long.bitCount(newdata);
					i++;
				}
				this.rlw.position += this.rlw.getNumberOfLiteralWords() + 1;

			}
		}
		
		

	}
	
	
	public void andHybrid(final HybridBitmap a, final HybridBitmap container) {
		container.verbatim=true;
		int j = 0;
		int i=0;
		int runLength=0;
		
		
		

		if (this.verbatim) { // this is verbatim
			container.reserve(this.actualsizeinwords);
			a.rlw = new RunningLengthWord(a.buffer, 0);
			
			while (i < this.actualsizeinwords) {
				runLength=(int) a.rlw.getRunningLength();
				if (a.rlw.getRunningBit()) { // fill of ones
					for (j = 0; j < runLength; j++) {
						
						container.buffer[i]=this.buffer[i];
						i++;
					}
				} else {
					//container.addStreamOfEmptyWords(false, a.rlw.getRunningLength());
					Arrays.fill(container.buffer, i, i+runLength, 0);
					i+=runLength; 
				}

				for( j=0; j<a.rlw.getNumberOfLiteralWords(); j++){					
					container.buffer[i]=this.buffer[i]& a.buffer[a.rlw.position+j+1];
					i++;
				}
				a.rlw.position += a.rlw.getNumberOfLiteralWords() + 1;
				

			}
		} else { // a is verbatim
			container.reserve(a.actualsizeinwords);
			this.rlw = new RunningLengthWord(this.buffer, 0);
			
			while (i < a.actualsizeinwords) {
				runLength=(int) this.rlw.getRunningLength();
				if (this.rlw.getRunningBit()) { // fill of ones
					for (j = 0; j < runLength; j++) {						
						container.buffer[i]=a.buffer[i];
						i++;
					}
				} else {
					//container.addStreamOfEmptyWords(false, a.rlw.getRunningLength());
					Arrays.fill(container.buffer, i, i+runLength, 0);
					i+=runLength; 
				}

				for( j=0; j<this.rlw.getNumberOfLiteralWords(); j++){					
					container.buffer[i]=a.buffer[i]& this.buffer[this.rlw.position+j+1];
					i++;
				}
				this.rlw.position += this.rlw.getNumberOfLiteralWords() + 1;
				

			}
		}
		
		container.actualsizeinwords=i;
		//container.sizeinbits=container.actualsizeinwords<<6;
		container.sizeinbits = Math.max(this.sizeinbits, a.sizeinbits);

	}
	
	

	public void orHybridCompress(final HybridBitmap a, final HybridBitmap container) {
		int j = 0;
		int i = 0;

		if (this.verbatim) { // this is verbatim
			container.reserve(this.actualsizeinwords);
			a.rlw = new RunningLengthWord(a.buffer, 0);

			while (i < this.actualsizeinwords) {
				if (a.rlw.getRunningBit()) { // fill of ones
					container.addStreamOfEmptyWords(true, a.rlw.getRunningLength());
					i += a.rlw.getRunningLength();

				} else {
					for (j = 0; j < a.rlw.getRunningLength(); j++) {
						container.add(this.buffer[i]);
						i++;
					}
				}

				for (j = 0; j < a.rlw.getNumberOfLiteralWords(); j++) {
					container.add(this.buffer[i] | a.buffer[a.rlw.position + j + 1]);
					i++;
				}
				a.rlw.position += a.rlw.getNumberOfLiteralWords() + 1;

			}
		} else { // a is verbatim
			container.reserve(a.actualsizeinwords);
			this.rlw = new RunningLengthWord(this.buffer, 0);

			while (i < a.actualsizeinwords) {
				if (this.rlw.getRunningBit()) { // fill of ones
					container.addStreamOfEmptyWords(true, this.rlw.getRunningLength());
					i += this.rlw.getRunningLength();

				} else {
					for (j = 0; j < this.rlw.getRunningLength(); j++) {
						container.add(a.buffer[i]);
						i++;
					}
				}

				for (j = 0; j < this.rlw.getNumberOfLiteralWords(); j++) {
					container.add(a.buffer[i] | this.buffer[this.rlw.position + j + 1]);
					i++;
				}
				this.rlw.position += this.rlw.getNumberOfLiteralWords() + 1;

			}
		}
		container.sizeinbits = Math.max(this.sizeinbits, a.sizeinbits);

	}
	
	
	public void orHybrid(final HybridBitmap a, final HybridBitmap container) {
		container.verbatim=true;
		int j = 0;
		int i=0;
		int runLength=0;
		
		

		if (this.verbatim) { // this is verbatim
			container.reserve(this.actualsizeinwords);
			a.rlw = new RunningLengthWord(a.buffer, 0);
			
			while (i < this.actualsizeinwords) {
				runLength=(int) a.rlw.getRunningLength();
				if (a.rlw.getRunningBit()) { // fill of ones
					Arrays.fill(container.buffer, i, i+runLength, ~0L);
					i+=runLength; 
				} else {
					//container.addStreamOfEmptyWords(false, a.rlw.getRunningLength());
					for (j = 0; j < runLength; j++) {						
						container.buffer[i]=this.buffer[i];
						i++;
					}
				}

				for( j=0; j<a.rlw.getNumberOfLiteralWords(); j++){					
					container.buffer[i]=this.buffer[i]| a.buffer[a.rlw.position+j+1];
					i++;
				}
				a.rlw.position += a.rlw.getNumberOfLiteralWords() + 1;				

			}
		} else { // a is verbatim
			container.reserve(a.actualsizeinwords);
			this.rlw = new RunningLengthWord(this.buffer, 0);
			
			while (i < a.actualsizeinwords) {
				runLength=(int) this.rlw.getRunningLength();
				if (this.rlw.getRunningBit()) { // fill of ones
//					for (j = 0; j < runLength; j++) {						
//						container.buffer[i]=this.buffer[i];
//						i++;
//					}
					Arrays.fill(container.buffer, i, i+runLength, ~0L);
					i+=runLength; 
				} else {
					//container.addStreamOfEmptyWords(false, a.rlw.getRunningLength());
					for (j = 0; j < runLength; j++) {						
						container.buffer[i]=a.buffer[i];
						i++;
					}
				}

				for( j=0; j<this.rlw.getNumberOfLiteralWords(); j++){					
					container.buffer[i]=a.buffer[i]| this.buffer[this.rlw.position+j+1];
					i++;
				}
				this.rlw.position += this.rlw.getNumberOfLiteralWords() + 1;				

			}
			container.sizeinbits = Math.max(this.sizeinbits, a.sizeinbits);
		}
		
		container.actualsizeinwords=i;
		//container.sizeinbits=container.actualsizeinwords<<6;
		container.sizeinbits = Math.max(this.sizeinbits, a.sizeinbits);

	}

	public void andNotHybridCompress(final HybridBitmap a, final HybridBitmap container) {
		int j = 0;
		int i=0;
		container.verbatim=false;
		

		if (this.verbatim) { // this is verbatim
			container.reserve(a.actualsizeinwords);
			a.rlw = new RunningLengthWord(a.buffer, 0);
			
			while (i < this.actualsizeinwords) {
				if (a.rlw.getRunningBit()) { // fill of ones
					//container.addStreamOfEmptyWords(false, a.rlw.getRunningLength());
					for (j = 0; j < a.rlw.getRunningLength(); j++) {						
						container.add(0);
						i++;
					}
					
				} else {
					for (j = 0; j < a.rlw.getRunningLength(); j++) {						
						container.add(this.buffer[i]);
						i++;
					}
					
				}
				for( j=0; j<a.rlw.getNumberOfLiteralWords(); j++){					
					container.add(this.buffer[i] & (~a.buffer[a.rlw.position + j + 1]));					
					i++;
				}
				a.rlw.position += a.rlw.getNumberOfLiteralWords() + 1;
			}
		} else { // a is verbatim
			container.reserve(this.actualsizeinwords);
			this.rlw = new RunningLengthWord(this.buffer, 0);			
			while (i < a.actualsizeinwords) {
				if (this.rlw.getRunningBit()) { // fill of ones
					for (j = 0; j < this.rlw.getRunningLength(); j++) {						
						container.add(~a.buffer[i]);						
						i++;
					}
				} else {
					container.addStreamOfEmptyWords(false, this.rlw.getRunningLength());
					i+=this.rlw.getRunningLength(); 
				}
				for( j=0; j<this.rlw.getNumberOfLiteralWords(); j++){				
					container.add((this.buffer[this.rlw.position + j + 1])& (~a.buffer[i]));					
					i++;
				}
				this.rlw.position += this.rlw.getNumberOfLiteralWords() + 1;

			}
		}
		container.sizeinbits = Math.max(this.sizeinbits, a.sizeinbits);
	}
	
	public void andNotHybrid(final HybridBitmap a, final HybridBitmap container) {
		container.verbatim=true;
		int j = 0;
		int i=0;
		int runLength=0;
		container.verbatim=true;
		
		

		if (this.verbatim) { // this is verbatim
			container.reserve(this.actualsizeinwords);
			a.rlw = new RunningLengthWord(a.buffer, 0);
			
			while (i < this.actualsizeinwords) {
				runLength=(int) a.rlw.getRunningLength();
				if (a.rlw.getRunningBit()) { // fill of ones
					Arrays.fill(container.buffer, i, i+runLength, 0);
					i+=runLength; 
					
				} else {
					//container.addStreamOfEmptyWords(false, a.rlw.getRunningLength());
					for (j = 0; j < runLength; j++) {						
						container.buffer[i]=this.buffer[i];
						i++;
					}
				}

				for( j=0; j<a.rlw.getNumberOfLiteralWords(); j++){					
					container.buffer[i]=this.buffer[i]& (~a.buffer[a.rlw.position+j+1]);
					i++;
				}
				a.rlw.position += a.rlw.getNumberOfLiteralWords() + 1;
				

			}
		} else { // a is verbatim
			container.reserve(a.actualsizeinwords);
			this.rlw = new RunningLengthWord(this.buffer, 0);
			
			while (i < a.actualsizeinwords) {
				runLength=(int) this.rlw.getRunningLength();
				if (this.rlw.getRunningBit()) { // fill of ones
					for (j = 0; j < runLength; j++) {						
						container.buffer[i]=(~a.buffer[i]);
						i++;
					}
				} else {
					//container.addStreamOfEmptyWords(false, a.rlw.getRunningLength());
					Arrays.fill(container.buffer, i, i+runLength, 0);
					i+=runLength; 
				}

				for( j=0; j<this.rlw.getNumberOfLiteralWords(); j++){					
					container.buffer[i]=this.buffer[this.rlw.position+j+1]& (~a.buffer[i]);
					i++;
				}
				this.rlw.position += this.rlw.getNumberOfLiteralWords() + 1;
				

			}
		}
		
		container.actualsizeinwords=i;
		//container.sizeinbits=container.actualsizeinwords<<6;
		container.sizeinbits = Math.max(this.sizeinbits, a.sizeinbits);

	}

	public void xorHybridCompress(final HybridBitmap a, final HybridBitmap container) {
		int j = 0;
		int i = 0;

		if (this.verbatim) { // this is verbatim
			container.reserve(this.actualsizeinwords);
			a.rlw = new RunningLengthWord(a.buffer, 0);

			while (i < this.actualsizeinwords) {
				if (a.rlw.getRunningBit()) { // fill of ones
					for (j = 0; j < a.rlw.getRunningLength(); j++) {
						container.add(~this.buffer[i]);
						i++;
					}
				} else {

					for (j = 0; j < a.rlw.getRunningLength(); j++) {
						container.add(this.buffer[i]);
						i++;
					}
				}

				for (j = 0; j < a.rlw.getNumberOfLiteralWords(); j++) {
					container.add(this.buffer[i] ^ a.buffer[a.rlw.position + j + 1]);
					i++;
				}
				a.rlw.position += a.rlw.getNumberOfLiteralWords() + 1;

			}
		} else { // a is verbatim
			container.reserve(a.actualsizeinwords);
			this.rlw = new RunningLengthWord(this.buffer, 0);

			while (i < a.actualsizeinwords) {
				if (this.rlw.getRunningBit()) { // fill of ones
					for (j = 0; j < this.rlw.getRunningLength(); j++) {
						container.add(~a.buffer[i]);
						i++;
					}
				} else {

					for (j = 0; j < this.rlw.getRunningLength(); j++) {
						container.add(a.buffer[i]);
						i++;
					}
				}

				for (j = 0; j < this.rlw.getNumberOfLiteralWords(); j++) {
					container.add(a.buffer[i] ^ this.buffer[this.rlw.position + j + 1]);
					i++;
				}
				this.rlw.position += this.rlw.getNumberOfLiteralWords() + 1;

			}

		}
		container.sizeinbits = Math.max(this.sizeinbits, a.sizeinbits);

	}
	
	public void xorHybrid(final HybridBitmap a, final HybridBitmap container) {
		container.verbatim=true;
		int j = 0;
		int i=0;
		int runLength=0;
		
		

		if (this.verbatim) { // this is verbatim
			container.reserve(this.actualsizeinwords);
			a.rlw = new RunningLengthWord(a.buffer, 0);
			
			while (i < this.actualsizeinwords) {
				runLength=(int) a.rlw.getRunningLength();
				if (a.rlw.getRunningBit()) { // fill of ones
					for (j = 0; j < runLength; j++) {						
						container.buffer[i]=~(this.buffer[i]);
						i++;
					}
				} else {
					for (j = 0; j < runLength; j++) {						
						container.buffer[i]=(this.buffer[i]);
						i++;
					}
				}

				for( j=0; j<a.rlw.getNumberOfLiteralWords(); j++){					
					container.buffer[i]=this.buffer[i]^(a.buffer[a.rlw.position+j+1]);
					i++;
				}
				a.rlw.position += a.rlw.getNumberOfLiteralWords() + 1;
				

			}
		} else { // a is verbatim
			container.reserve(a.actualsizeinwords);
			this.rlw = new RunningLengthWord(this.buffer, 0);
			
			while (i < a.actualsizeinwords) {
				runLength=(int) this.rlw.getRunningLength();
				if (this.rlw.getRunningBit()) { // fill of ones
					for (j = 0; j < runLength; j++) {						
						container.buffer[i]=~(a.buffer[i]);
						i++;
					}
				} else {
					for (j = 0; j < runLength; j++) {						
						container.buffer[i]=(a.buffer[i]);
						i++;
					}
				}

				for( j=0; j<this.rlw.getNumberOfLiteralWords(); j++){					
					container.buffer[i]=a.buffer[i]^(this.buffer[this.rlw.position+j+1]);
					i++;
				}
				this.rlw.position += this.rlw.getNumberOfLiteralWords() + 1;
				

			}
			
		}
		
		container.actualsizeinwords=i;
		//container.sizeinbits=container.actualsizeinwords<<6;
		container.sizeinbits = Math.max(this.sizeinbits, a.sizeinbits);

	}


	public void setVerbatim(boolean verbatimFlag) {
		this.verbatim = verbatimFlag;
	}

  
  /**
   * Returns the cardinality of the result of a bitwise AND of the values of the
   * current bitmap with some other bitmap. Avoids needing to allocate an
   * intermediate bitmap to hold the result of the OR.
   * 
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @return the cardinality
   */
  public int andCardinality(final HybridBitmap a) {
    final BitCounter counter = new BitCounter();
    andToContainer(a, counter);
    return counter.getCount();
  }

  /**
   * Returns a new compressed bitmap containing the bitwise AND NOT values of
   * the current bitmap with some other bitmap.
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @param a
   *          the other bitmap
   * @return the EWAH compressed bitmap
   */
	// public EWAHCompressedBitmap andNot(final EWAHCompressedBitmap a) {
	// final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
	//
	// if (this.verbatim && a.verbatim) {
	// if (this.setbits / (this.getSize() * 8) < 0.0001 || a.setbits /
	// (a.getSize() * 8) < 0.000001) {
	// container.reserve(this.actualsizeinwords << 1);
	// andNotVerbatimCompress(a, container);
	// } else {
	// container.reserve(this.actualsizeinwords);
	// andNotVerbatim(a, container);
	//
	// }
	// } else if (this.verbatim || a.verbatim) {
	// andNotHybrid(a, container);
	//
	// } else {
	// container.reserve(this.actualsizeinwords > a.actualsizeinwords ?
	// this.actualsizeinwords : a.actualsizeinwords);
	// andNotToContainer(a, container);
	//
	// }
	// return container;
	// }

  /**
   * Returns a new compressed bitmap containing the bitwise AND NOT values of
   * the current bitmap with some other bitmap. This method is expected to
   * be faster than doing A.and(((EWAHCompressedBitmap) B.clone()).not()).
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @return the EWAH compressed bitmap
   */
  public void andNotToContainer(final HybridBitmap a,
    final HybridBitmap container) {
    final EWAHIterator i = getEWAHIterator();
    final EWAHIterator j = a.getEWAHIterator();
    final IteratingBufferedRunningLengthWord rlwi = new IteratingBufferedRunningLengthWord(i);
    final IteratingBufferedRunningLengthWord rlwj = new IteratingBufferedRunningLengthWord(j);
    
    while ((rlwi.size()>0) && (rlwj.size()>0)) { // both have words remaining    	
      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) { //at least one has a fill word with remaining length
        final boolean i_is_prey = rlwi.getRunningLength() < rlwj.getRunningLength(); // is prey - this has a smaller running length than a
        final IteratingBufferedRunningLengthWord prey = i_is_prey ? rlwi : rlwj; // rlwi if is_prey. Otherwise rlwj
        final IteratingBufferedRunningLengthWord predator = i_is_prey ? rlwj
          : rlwi;
        if (  ((predator.getRunningBit() == true) && (i_is_prey))
          || ((predator.getRunningBit() == false) && (!i_is_prey))){
        	
          container.addStreamOfEmptyWords(false, predator.getRunningLength());
          
          prey.discardFirstWords(predator.getRunningLength());
          predator.discardFirstWords(predator.getRunningLength());
        } else if (i_is_prey) {
          long index = prey.discharge(container, predator.getRunningLength()); 
          
          container.addStreamOfEmptyWords(false, predator.getRunningLength()
            - index);
          
          predator.discardFirstWords(predator.getRunningLength());
        } else { //if i is predator and the running bit is true
          long index = prey.dischargeNegated(container, predator.getRunningLength()); 
          container.addStreamOfEmptyWords(true, predator.getRunningLength()
            - index);
         
          predator.discardFirstWords(predator.getRunningLength());          
        }
      }
      final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(),
        rlwj.getNumberOfLiteralWords());
      if (nbre_literal > 0) {
        for (int k = 0; k < nbre_literal; ++k){
        	
          container.add(rlwi.getLiteralWordAt(k) & (~rlwj.getLiteralWordAt(k)));
         
         
        
        }
        rlwi.discardFirstWords(nbre_literal);
        rlwj.discardFirstWords(nbre_literal);
      }
    }
    final boolean i_remains = rlwi.size()>0;
    final IteratingBufferedRunningLengthWord remaining = i_remains ? rlwi : rlwj;
    if(i_remains)
      remaining.discharge(container);
    else
      remaining.dischargeAsEmpty(container);
    container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
  }

  /**
   * Returns the cardinality of the result of a bitwise AND NOT of the values of
   * the current bitmap with some other bitmap. Avoids needing to allocate an
   * intermediate bitmap to hold the result of the OR.
   * 
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @return the cardinality
   */
//  public int andNotCardinality(final EWAHCompressedBitmap a) {
//    final BitCounter counter = new BitCounter();
//    andNotToContainer(a, counter);
//    return counter.getCount();
//  }

  /**
   * reports the number of bits set to true. Running time is proportional to
   * compressed size (as reported by sizeInBytes).
   * 
   * @return the number of bits set to true
   */
  public int cardinality() {
    int counter = 0;
		if (this.verbatim) {
			for (int i = 0; i < this.actualsizeinwords; i++) {
				counter += Long.bitCount(this.buffer[i]);
			}
		} else {

    final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
    while (i.hasNext()) {
      RunningLengthWord localrlw = i.next();
      if (localrlw.getRunningBit()) {
        counter += wordinbits * localrlw.getRunningLength();
      }
      for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
        counter += Long.bitCount(i.buffer()[i.literalWords() + j]);
      }
    }
		}
    return counter;
  }
  

  /**
   * count the total number of bits in the compressed bitmap. This is for debugging.
   * 
   */
  
  
  public int countNumberOfBits(){
		
	  if (this.verbatim)
	  return actualsizeinwords*64;
	  else{
		  final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
		  int counter=0;
		  while (i.hasNext()) {
		      RunningLengthWord localrlw = i.next();		      
		        counter += wordinbits * localrlw.getRunningLength();
		        counter+=(64*localrlw.getNumberOfLiteralWords());		     
		    } 
		 return counter;
	  }
	
		  
	  
  }
  
  
  public boolean getBit(int pos){
	  int localPos = pos%wordinbits;
	  int wordPos = pos/wordinbits;
	  int curPos=0;
	  if(this.verbatim){		  
		  long word = this.buffer[wordPos];
		  if(((word>>localPos)&1)!=0)		  
		  return true;
		  else return false;
	  }
	  else{
		  this.rlw.position=0;
		  boolean res=false;
		  while(curPos<=wordPos){			  
			  if(curPos+this.rlw.getRunningLength()>wordPos){
				  res = this.rlw.getRunningBit();
			  curPos+=this.rlw.getRunningLength();}
			  else{
				  curPos+=this.rlw.getRunningLength();				  				  
				  if(curPos+this.rlw.getNumberOfLiteralWords()>=wordPos){
					  long word = this.buffer[this.rlw.position+(wordPos-curPos)+1];
					  if(((word>>localPos)&1)!=0)		  
						   res= true;
					  //else res= false;
				  }
				  curPos+=this.rlw.getNumberOfLiteralWords();
				 this.rlw.position+=this.rlw.getNumberOfLiteralWords()+1;
			  }  
			  
		  }
		  
		  return res;
		  
	  }
	  
  }

  /**
   * Clear any set bits and set size in bits back to 0
   */
  public void clear() {
    this.sizeinbits = 0;
    this.actualsizeinwords = 1;
    this.rlw.position = 0;
    // buffer is not fully cleared but any new set operations should overwrite
    // stale data
    this.buffer[0] = 0;
  }

  /*
   * @see java.lang.Object#clone()
   */
  @Override
  public Object clone() throws java.lang.CloneNotSupportedException {
    final HybridBitmap clone = (HybridBitmap) super.clone();
    clone.buffer = this.buffer.clone();
    clone.rlw = new RunningLengthWord(clone.buffer, this.rlw.position);
    clone.actualsizeinwords = this.actualsizeinwords;
    clone.sizeinbits = this.sizeinbits;
    clone.density=this.density;
    clone.verbatim = this.verbatim;
    return clone;
  }

  /**
   * Deserialize.
   * 
   * @param in
   *          the DataInput stream
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public void deserialize(DataInput in) throws IOException {
    this.sizeinbits = in.readInt();
    this.actualsizeinwords = in.readInt();
    if (this.buffer.length < this.actualsizeinwords) {
      this.buffer = new long[this.actualsizeinwords];
    }
    for (int k = 0; k < this.actualsizeinwords; ++k)
      this.buffer[k] = in.readLong();
    this.rlw = new RunningLengthWord(this.buffer, in.readInt());
  }

  /**
   * Check to see whether the two compressed bitmaps contain the same set bits.
   * 
   * @author Colby Ranger
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (o instanceof HybridBitmap) {
      try {
        this.xorToContainer((HybridBitmap) o, new NonEmptyVirtualStorage());
        return true;
      } catch (NonEmptyVirtualStorage.NonEmptyException e) {
        return false;
      }
    }
    return false;
  }

  /**
   * For experts: You want to add many zeroes or ones faster?
   * 
   * This method does not update sizeinbits.
   * 
   * @param v
   *          the boolean value
   * @param number
   *          the number (must be greater than 0)
   * @return nothing
   */
  private void fastaddStreamOfEmptyWords(final boolean v, long number) {
    if ((this.rlw.getRunningBit() != v) && (this.rlw.size() == 0)) {
      this.rlw.setRunningBit(v);
    } else if ((this.rlw.getNumberOfLiteralWords() != 0)
      || (this.rlw.getRunningBit() != v)) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      if (v)
        this.rlw.setRunningBit(v);
    }

    final long runlen = this.rlw.getRunningLength();
    final long whatwecanadd = number < RunningLengthWord.largestrunninglengthcount
      - runlen ? number : RunningLengthWord.largestrunninglengthcount - runlen;
    this.rlw.setRunningLength(runlen + whatwecanadd);
    number -= whatwecanadd;

    while (number >= RunningLengthWord.largestrunninglengthcount) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      if (v)
        this.rlw.setRunningBit(v);
      this.rlw.setRunningLength(RunningLengthWord.largestrunninglengthcount);
      number -= RunningLengthWord.largestrunninglengthcount;
    }
    if (number > 0) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      if (v)
        this.rlw.setRunningBit(v);
      this.rlw.setRunningLength(number);
    }
  }

  /**
   * Gets an EWAHIterator over the data. This is a customized iterator which
   * iterates over run length word. For experts only.
   * 
   * @return the EWAHIterator
   */
  public EWAHIterator getEWAHIterator() {
    return new EWAHIterator(this.buffer, this.actualsizeinwords);
  }

  public long[] getBuffer() {
		return this.buffer;
	}
  
  
  /**
   * get the locations of the true values as one vector. (may use more memory
   * than iterator())
   * 
   * @return the positions
   */
  public int[] getPositions() {
    int[] v = new int[this.cardinality()];
		if (this.verbatim) {
			int ntz = 0;
			long data = 0;
			int vpos=0;
			int pos=1;
			for (int i = 0; i < this.actualsizeinwords; i++) {
				data = this.buffer[i];
				//if (data > 0) {
				while (data != 0) {
				       ntz = Long.numberOfTrailingZeros(data);
				       data ^= (1l << ntz);
				       v[vpos++]=1+ntz+pos;
				      // v.add(new Integer(1+ntz + pos));
				                     }
				pos+=64;


				//}
			}
			return v;
		} else {

    final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
    int pos = 1;
    int ntz;
    int vpos=0;
    while (i.hasNext()) {
      RunningLengthWord localrlw = i.next();
      if (localrlw.getRunningBit()) {
        for (int j = 0; j < localrlw.getRunningLength(); ++j) {
          for (int c = 0; c < wordinbits; ++c)
          //  v.add(new Integer(pos++));
        	  v[vpos++]=pos++;
        }
      } else {
        pos += wordinbits * localrlw.getRunningLength();
      }
      for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
        long data = i.buffer()[i.literalWords() + j];
      
        
        while (data != 0) {
            ntz = Long.numberOfTrailingZeros(data);
            data ^= (1l << ntz);
            v[vpos++]= 1+ntz+pos;
           // v.add(new Integer(1+ntz + pos));
                          }

        pos += wordinbits;
      }
    }
   
    return v;
		}
  }
  
  
  /**
   * get the locations of the true values as one vector. (may use more memory
   * than iterator())
   * 
   * @return the positions
   */
  public int[] getPositionsOptimized() {
   // final ArrayList<Integer> v = new ArrayList<Integer>();
	  int[] v = new int[this.cardinality()];
		if (this.verbatim) {
			int pos = 1;
			int vpos=0;
			long data = 0;
			for (int i = 0; i < this.actualsizeinwords; i++) {
				data = this.buffer[i];
				//if (data > 0) {
					while (data != 0) {											
						//v.add(Long.bitCount(~(data^(-data)))+pos);
						v[vpos++]=Long.bitCount(~(data^(-data)))+pos;
						
						data = data&(data-1);	
						
					}
					pos+=64;

				//}
			}
			return v;
		} else {

    final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
    int pos = 1;
    int vpos=0;
    while (i.hasNext()) {
      RunningLengthWord localrlw = i.next();
      if (localrlw.getRunningBit()) {
        for (int j = 0; j < localrlw.getRunningLength(); ++j) {
          for (int c = 0; c < wordinbits; ++c)
           // v.add(new Integer(pos++));
        	  v[vpos++]=pos++;
        }
      } else {
        pos += wordinbits * localrlw.getRunningLength();
      }
      for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
        long data = i.buffer()[i.literalWords() + j];
        while (data != 0) {											
			//v.add(Long.bitCount(~(data^(-data)))+pos);
        	v[vpos++]=Long.bitCount(~(data^(-data)))+pos;
			data = data&(data-1);	
		}
		pos+=64;
      }
    }
    
    return v;
		}
  }
  
  /**
   * get the locations of the true values as one vector and add the offset to return the position including the offset. (may use more memory
   * than iterator())
   * 
   * @return the positions
   */
  public List<Integer> getPositions( int offset) {
    final ArrayList<Integer> v = new ArrayList<Integer>();
		if (this.verbatim) {
			int ntz = 0;
			long data = 0;
			for (int i = 0; i < this.actualsizeinwords; i++) {
				data = this.buffer[i];
				//if (data > 0) {
					while (data != 0) {
						ntz = Long.numberOfTrailingZeros(data);
						data ^= (1l << ntz);
						v.add(new Integer(1+ntz + (i * wordinbits))+offset);
					}

				//}
			}
			return v;
		} else {

    final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
    int pos = 1;
    while (i.hasNext()) {
      RunningLengthWord localrlw = i.next();
      if (localrlw.getRunningBit()) {
        for (int j = 0; j < localrlw.getRunningLength(); ++j) {
          for (int c = 0; c < wordinbits; ++c)
            v.add(new Integer(pos++)+offset);
        }
      } else {
        pos += wordinbits * localrlw.getRunningLength();
      }
      for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
        long data = i.buffer()[i.literalWords() + j];
        while (data != 0) {
          final int ntz = Long.numberOfTrailingZeros(data);
          data ^= (1l << ntz);
          v.add(new Integer(ntz + pos)+offset);
        }
        pos += wordinbits;
      }
    }
    while ((v.size() > 0)
      && (v.get(v.size() - 1).intValue() >= this.sizeinbits))
      v.remove(v.size() - 1);
    return v;
		}
  }

  /**
   * Returns a customized hash code (based on Karp-Rabin). Naturally, if the
   * bitmaps are equal, they will hash to the same value.
   * 
   */
  @Override
  public int hashCode() {
    int karprabin = 0;
    final int B = 31;
    final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
    while( i.hasNext() ) {
      i.next();
      if (i.rlw.getRunningBit() == true) {
        karprabin += B * karprabin
          + (i.rlw.getRunningLength() & ((1l << 32) - 1));
        karprabin += B * karprabin + (i.rlw.getRunningLength() >>> 32);
      }
      for (int k = 0; k <  i.rlw.getNumberOfLiteralWords(); ++k) {
        karprabin += B * karprabin + (this.buffer[i.literalWords() + k] & ((1l << 32) - 1));
        karprabin += B * karprabin + (this.buffer[i.literalWords() + k] >>> 32);
      }
    }
    return karprabin;
  }

  /**
   * Return true if the two EWAHCompressedBitmap have both at least one true bit
   * in the same position. Equivalently, you could call "and" and check whether
   * there is a set bit, but intersects will run faster if you don't need the
   * result of the "and" operation.
   * 
   * @since 0.3.2
   * @param a
   *          the other bitmap
   * @return whether they intersect
   */
  public boolean intersects(final HybridBitmap a) {
    NonEmptyVirtualStorage nevs = new NonEmptyVirtualStorage();
    try {
      this.andToContainer(a, nevs);
    } catch (NonEmptyVirtualStorage.NonEmptyException nee) {
      return true;
    }
    return false;
  }

  /**
   * Iterator over the set bits (this is what most people will want to use to
   * browse the content if they want an iterator). The location of the set bits
   * is returned, in increasing order.
   * 
   * @return the int iterator
   */
  public IntIterator intIterator() {
    return new IntIteratorImpl(
        new EWAHIterator(this.buffer, this.actualsizeinwords));
  }

  /**
   * iterate over the positions of the true values. This is similar to
   * intIterator(), but it uses Java generics.
   * 
   * @return the iterator
   */
  public Iterator<Integer> iterator() {
    return new Iterator<Integer>() {
      public boolean hasNext() {
        return this.under.hasNext();
      }

      public Integer next() {
        return new Integer(this.under.next());
      }

      public void remove() {
        throw new UnsupportedOperationException("bitsets do not support remove");
      }

      final private IntIterator under = intIterator();
    };
  }

  /**
   * For internal use.
   * 
   * @param data
   *          the array of words to be added
   * @param start
   *          the starting point
   * @param number
   *          the number of words to add
   */
  private void negative_push_back(final long[] data, final int start,
    final int number) {
    while (this.actualsizeinwords + number >= this.buffer.length) {
      final long oldbuffer[] = this.buffer;
      this.buffer = new long[oldbuffer.length * 2];
      System.arraycopy(oldbuffer, 0, this.buffer, 0, oldbuffer.length);
      this.rlw.array = this.buffer;
    }
    for (int k = 0; k < number; ++k)
      this.buffer[this.actualsizeinwords + k] = ~data[start + k];
    this.actualsizeinwords += number;
  }

  /**
   * Negate (bitwise) the current bitmap. To get a negated copy, do
   * EWAHCompressedBitmap x= ((EWAHCompressedBitmap) mybitmap.clone()); x.not();
   * 
   * The running time is proportional to the compressed size (as reported by
   * sizeInBytes()).
   * 
   */
  public void not() {
	  if (this.verbatim){
		  for(int i=0; i < this.buffer.length; i++){
			  this.buffer[i]=~this.buffer[i];
		  }
	  }else{
    final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
    if (!i.hasNext())
      return;
 
    while (true) {
    	final RunningLengthWord rlw1 = i.next();
      rlw1.setRunningBit(!rlw1.getRunningBit());
      for (int j = 0; j < rlw1.getNumberOfLiteralWords(); ++j) {
        i.buffer()[i.literalWords() + j] = ~i.buffer()[i.literalWords() + j];
      }

      if (!i.hasNext()) {// must potentially adjust the last literal word
          final int usedbitsinlast = this.sizeinbits % wordinbits;
          if (usedbitsinlast == 0)
            return;

    	if (rlw1.getNumberOfLiteralWords() == 0) {
    		if((rlw1.getRunningLength()>0) && (rlw1.getRunningBit())) {
    			rlw1.setRunningLength(rlw1.getRunningLength()-1);
    			this.addLiteralWord((~0l) >>> (wordinbits - usedbitsinlast));
    		}
          return;
    	}
        i.buffer()[i.literalWords() + rlw1.getNumberOfLiteralWords() - 1] &= ((~0l) >>> (wordinbits - usedbitsinlast));
        return;
      }
    }
  }}

  /**
   * Returns a new compressed bitmap containing the bitwise OR values of the
   * current bitmap with some other bitmap.
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @param a
   *          the other bitmap
   * @return the EWAH compressed bitmap
   */
	// public EWAHCompressedBitmap or(final EWAHCompressedBitmap a) {
	// final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
	//
	// if (this.verbatim && a.verbatim) {
	// if (this.setbits / (this.getSize() * 8) < 0.000001 || a.setbits /
	// (a.getSize() * 8) < 0.000001) {
	// container.reserve(this.actualsizeinwords << 1);
	// orVerbatimCompress(a, container);
	// } else {
	// container.reserve(this.actualsizeinwords);
	// orVerbatim(a, container);
	// }
	// } else if (this.verbatim || a.verbatim) {
	// orHybrid(a, container);
	// } else {
	// container.reserve(this.actualsizeinwords + a.actualsizeinwords);
	// orToContainer(a, container);
	// }
	// return container;
	// }

  /**
   * Computes the bitwise or between the current bitmap and the bitmap "a".
   * Stores the result in the container.
   * 
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @param container
   *          where we store the result
   */
  public void orToContainer(final HybridBitmap a, final BitmapStorage container) {
    final EWAHIterator i = a.getEWAHIterator();
    final EWAHIterator j = getEWAHIterator();
    final IteratingBufferedRunningLengthWord rlwi = new IteratingBufferedRunningLengthWord(i);
    final IteratingBufferedRunningLengthWord rlwj = new IteratingBufferedRunningLengthWord(j);
    while ((rlwi.size()>0) && (rlwj.size()>0)) {
      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
        final boolean i_is_prey = rlwi.getRunningLength() < rlwj
          .getRunningLength();
        final IteratingBufferedRunningLengthWord prey = i_is_prey ? rlwi
          : rlwj;
        final IteratingBufferedRunningLengthWord predator = i_is_prey ? rlwj
          : rlwi;
        if (predator.getRunningBit() == true) {
          container.addStreamOfEmptyWords(true, predator.getRunningLength());
          prey.discardFirstWords(predator.getRunningLength());
          predator.discardFirstWords(predator.getRunningLength());
        } else {
          long index = prey.discharge(container, predator.getRunningLength());
          container.addStreamOfEmptyWords(false, predator.getRunningLength()
            - index);
          predator.discardFirstWords(predator.getRunningLength());
        }
      }
      final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(),
        rlwj.getNumberOfLiteralWords());
      if (nbre_literal > 0) {
        for (int k = 0; k < nbre_literal; ++k) {
          container.add(rlwi.getLiteralWordAt(k) | rlwj.getLiteralWordAt(k));
        }
        rlwi.discardFirstWords(nbre_literal);
        rlwj.discardFirstWords(nbre_literal);
      }
    }
    final boolean i_remains = rlwi.size()>0;
    final IteratingBufferedRunningLengthWord remaining = i_remains ? rlwi : rlwj;
    remaining.discharge(container);
    container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
  }
  
  public void orToContainerDecompress(final HybridBitmap a, final HybridBitmap container) {
	  container.reserve(((this.sizeinbits>>>6)+1));
	 // long runLength=0;
	 
	  container.verbatim=true;
	    final EWAHIterator i = a.getEWAHIterator();
	    final EWAHIterator j = getEWAHIterator();
	    container.actualsizeinwords=0;
	    final IteratingBufferedRunningLengthWord rlwi = new IteratingBufferedRunningLengthWord(i);
	    final IteratingBufferedRunningLengthWord rlwj = new IteratingBufferedRunningLengthWord(j);
	    while ((rlwi.size()>0) && (rlwj.size()>0)) {
	      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
	    //	  System.out.println(container.getPositions());
	        final boolean i_is_prey = rlwi.getRunningLength() < rlwj
	          .getRunningLength();
	        final IteratingBufferedRunningLengthWord prey = i_is_prey ? rlwi
	          : rlwj;
	        final IteratingBufferedRunningLengthWord predator = i_is_prey ? rlwj
	          : rlwi;
	        if (predator.getRunningBit() == true) {
	        	Arrays.fill(container.buffer, container.actualsizeinwords, (int) (container.actualsizeinwords+predator.getRunningLength()), ~0L);
	          //container.addStreamOfEmptyWords(true, predator.getRunningLength());
	        	container.actualsizeinwords+=predator.getRunningLength();
	        	prey.discardFirstWords(predator.getRunningLength());
	          predator.discardFirstWords(predator.getRunningLength());
	         // container.actualsizeinwords+=predator.getRunningLength();
	        } else {
	        	//runLength=predator.getRunningLength();
	          long index = prey.dischargeDecompressed(container, predator.getRunningLength());
	          Arrays.fill(container.buffer, container.actualsizeinwords, (int) (container.actualsizeinwords+predator.getRunningLength()-index), 0);
	          //container.addStreamOfEmptyWords(false, predator.getRunningLength()
	         //   - index);
	          container.actualsizeinwords+=predator.getRunningLength()-index;
	          predator.discardFirstWords(predator.getRunningLength());
	          
	        }
	      }
	      final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(),
	        rlwj.getNumberOfLiteralWords());
	      if (nbre_literal > 0) {
	        for (int k = 0; k < nbre_literal; ++k) {
	          container.buffer[container.actualsizeinwords]=(rlwi.getLiteralWordAt(k) | rlwj.getLiteralWordAt(k));
	          container.actualsizeinwords++;
	        }
	        rlwi.discardFirstWords(nbre_literal);
	        rlwj.discardFirstWords(nbre_literal);
	      }
	    }
	    container.sizeinbits=Math.max(sizeInBits(), a.sizeInBits());
	   // System.out.println("container size: "+container.actualsizeinwords);
	   // final boolean i_remains = rlwi.size()>0;
	  //  final IteratingBufferedRunningLengthWord remaining = i_remains ? rlwi : rlwj;
	   // remaining.discharge(container);
	   // container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
	  }
  
  
//  public void orToContainerDecompress(final EWAHCompressedBitmap a, final EWAHCompressedBitmap container) {
//	  container.verbatim=true;
//	  int runLength=0;
//	  int i=0;
//	  int thisRunLength=0;
//	  int thisNumLiterals=0;
//	  int aRunlength=0;
//	  int aNumLIterals=0;
//	  RunningLengthWord shorter;
//	  RunningLengthWord longer;
//	  while(i<(this.sizeinbits>>>6)){
//		 shorter =(this.rlw.getRunningLength()<a.rlw.getRunningLength())?this.rlw:a.rlw;
//		 longer = 
//		 
//		  
//		  
//	  }
//	  }

  /**
   * Returns the cardinality of the result of a bitwise OR of the values of the
   * current bitmap with some other bitmap. Avoids needing to allocate an
   * intermediate bitmap to hold the result of the OR.
   * 
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @return the cardinality
   */
  public int orCardinality(final HybridBitmap a) {
    final BitCounter counter = new BitCounter();
    orToContainer(a, counter);
    return counter.getCount();
  }

  /**
   * For internal use.
   * 
   * @param data
   *          the word to be added
   */
  private void push_back(final long data) {
    if (this.actualsizeinwords == this.buffer.length) {
      final long oldbuffer[] = this.buffer;
      this.buffer = new long[oldbuffer.length * 2];
      System.arraycopy(oldbuffer, 0, this.buffer, 0, oldbuffer.length);
      this.rlw.array = this.buffer;
    }
    this.buffer[this.actualsizeinwords++] = data;
  }

  /**
   * For internal use.
   * 
   * @param data
   *          the array of words to be added
   * @param start
   *          the starting point
   * @param number
   *          the number of words to add
   */
  private void push_back(final long[] data, final int start, final int number) {
    while (this.actualsizeinwords + number >= this.buffer.length) {
      final long oldbuffer[] = this.buffer;
      this.buffer = new long[oldbuffer.length * 2];
      System.arraycopy(oldbuffer, 0, this.buffer, 0, oldbuffer.length);
      this.rlw.array = this.buffer;
    }
    System.arraycopy(data, start, this.buffer, this.actualsizeinwords, number);
    this.actualsizeinwords += number;
  }

  /*
   * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
   */
  public void readExternal(ObjectInput in) throws IOException {
    deserialize(in);
  }

  /**
   * For internal use (trading off memory for speed).
   * 
   * @param size
   *          the number of words to allocate
   * @return True if the operation was a success.
   */
  private boolean reserve(final int size) {
    if (size > this.buffer.length) {
      final long oldbuffer[] = this.buffer;
      this.buffer = new long[size];
      System.arraycopy(oldbuffer, 0, this.buffer, 0, oldbuffer.length);
      this.rlw.array = this.buffer;
      return true;
    }
    return false;
  }

  /**
   * Serialize.
   * 
   * @param out
   *          the DataOutput stream
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public void serialize(DataOutput out) throws IOException {
    out.writeInt(this.sizeinbits);
    out.writeInt(this.actualsizeinwords);
    for (int k = 0; k < this.actualsizeinwords; ++k)
      out.writeLong(this.buffer[k]);
    out.writeInt(this.rlw.position);
  }

  /**
   * Report the size required to serialize this bitmap
   * 
   * @return the size in bytes
   */
  public int serializedSizeInBytes() {
    return this.sizeInBytes() + 3 * 4;
  }
  
 

  /**
   * set the bit at position i to true, the bits must be set in increasing
   * order. For example, set(15) and then set(7) will fail. You must do set(7)
   * and then set(15).
   * 
   * @param i
   *          the index
   * @return true if the value was set (always true when i>= sizeInBits()).
   * @throws IndexOutOfBoundsException
   *           if i is negative or greater than Integer.MAX_VALUE - 64
   */
  public boolean set(final int i) {
    if ((i > Integer.MAX_VALUE - wordinbits) || (i < 0))
      throw new IndexOutOfBoundsException("Set values should be between 0 and "
        + (Integer.MAX_VALUE - wordinbits));
    if (i < this.sizeinbits)
      return false;
    // distance in words:
    final int dist = (i + wordinbits) / wordinbits
      - (this.sizeinbits + wordinbits - 1) / wordinbits;
    this.sizeinbits = i + 1;
    if (dist > 0) {// easy
      if (dist > 1)
        fastaddStreamOfEmptyWords(false, dist - 1);
      addLiteralWord(1l << (i % wordinbits));
      return true;
    }
    if (this.rlw.getNumberOfLiteralWords() == 0) {
      this.rlw.setRunningLength(this.rlw.getRunningLength() - 1);
      addLiteralWord(1l << (i % wordinbits));
      return true;
    }
    this.buffer[this.actualsizeinwords - 1] |= 1l << (i % wordinbits);
    if (this.buffer[this.actualsizeinwords - 1] == ~0l) {
      this.buffer[this.actualsizeinwords - 1] = 0;
      --this.actualsizeinwords;
      this.rlw.setNumberOfLiteralWords(this.rlw.getNumberOfLiteralWords() - 1);
      // next we add one clean word
      addEmptyWord(true);
    }
    return true;
  }

  /**
   * set the size in bits
   * 
   * @since 0.4.0
   */
  public void setSizeInBits(final int size) {
    this.sizeinbits = size;
  }

  /**
   * Change the reported size in bits of the *uncompressed* bitmap represented
   * by this compressed bitmap. It is not possible to reduce the sizeInBits, but
   * it can be extended. The new bits are set to false or true depending on the
   * value of defaultvalue.
   * 
   * @param size
   *          the size in bits
   * @param defaultvalue
   *          the default boolean value
   * @return true if the update was possible
   */
  public boolean setSizeInBits(final int size, final boolean defaultvalue) {
	 if (size < this.sizeinbits)
      return false;
    if (defaultvalue == false)
      extendEmptyBits(this, this.sizeinbits, size);
    else {
      // next bit could be optimized
      while (((this.sizeinbits % wordinbits) != 0) && (this.sizeinbits < size)) {
        	this.set(this.sizeinbits);
      }
      this.addStreamOfEmptyWords(defaultvalue, (size / wordinbits)
        - this.sizeinbits / wordinbits);
      // next bit could be optimized
      while (this.sizeinbits < size) {
        	this.set(this.sizeinbits);
      }
    }
    this.sizeinbits = size;
    return true;
  }

  /**
   * Returns the size in bits of the *uncompressed* bitmap represented by this
   * compressed bitmap. Initially, the sizeInBits is zero. It is extended
   * automatically when you set bits to true.
   * 
   * @return the size in bits
   */
  public int sizeInBits() {
    return this.sizeinbits;
  }

  /**
   * Report the *compressed* size of the bitmap (equivalent to memory usage,
   * after accounting for some overhead).
   * 
   * @return the size in bytes
   */
  public int sizeInBytes() {
    return this.actualsizeinwords * (wordinbits / 8);
  }

  /**
   * Populate an array of (sorted integers) corresponding to the location of the
   * set bits.
   * 
   * @return the array containing the location of the set bits
   */
  public int[] toArray() {
    int[] ans = new int[this.cardinality()];
    int inanspos = 0;
    int pos = 0;
    final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
    while (i.hasNext()) {
      RunningLengthWord localrlw = i.next();
      if (localrlw.getRunningBit()) {
        for (int j = 0; j < localrlw.getRunningLength(); ++j) {
          for (int c = 0; c < wordinbits; ++c) {
            ans[inanspos++] = pos++;
          }
        }
      } else {
        pos += wordinbits * localrlw.getRunningLength();
      }
      for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
        long data = i.buffer()[i.literalWords() + j];
        if (!usetrailingzeros) {
          for (int c = 0; c < wordinbits; ++c) {
            if ((data & (1l << c)) != 0)
              ans[inanspos++] = c + pos;
          }
          pos += wordinbits;
        } else {
          while (data != 0) {
            final int ntz = Long.numberOfTrailingZeros(data);
            data ^= (1l << ntz);
            ans[inanspos++] = ntz + pos;
          }
          pos += wordinbits;
        }
      }
    }
    return ans;

  }
  
  
  
  
  

  /**
   * A more detailed string describing the bitmap (useful for debugging).
   * 
   * @return the string
   */
  public String toDebugString() {
    String ans = " EWAHCompressedBitmap, size in bits = " + this.sizeinbits
      + " size in words = " + this.actualsizeinwords + "\n";
    final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
    while (i.hasNext()) {
      RunningLengthWord localrlw = i.next();
      if (localrlw.getRunningBit()) {
        ans += localrlw.getRunningLength() + " 1x11\n";
      } else {
        ans += localrlw.getRunningLength() + " 0x00\n";
      }
      ans += localrlw.getNumberOfLiteralWords() + " dirties\n";
      for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
        long data = i.buffer()[i.literalWords() + j];
        ans += "\t" + data + "\n";
      }
    }
    return ans;
  }

  /**
   * A string describing the bitmap.
   * 
   * @return the string
   */
  @Override
  public String toString() {
		StringBuffer answer = new StringBuffer();
		IntIterator i = this.intIterator();
		answer.append("{");
		if (i.hasNext())
			answer.append(i.next());
		while (i.hasNext()) {
			answer.append(",");
			answer.append(i.next());
		}
		answer.append("}");
		return answer.toString();
  }

  /*
   * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    serialize(out);
  }

  /**
   * Returns a new compressed bitmap containing the bitwise XOR values of the
   * current bitmap with some other bitmap.
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @param a
   *          the other bitmap
   * @return the EWAH compressed bitmap
   */
	// public EWAHCompressedBitmap xor(final EWAHCompressedBitmap a) {
	// final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
	//
	// if (this.verbatim && a.verbatim) {
	// if (this.setbits / (this.getSize() * 8) < 0.000001 || a.setbits /
	// (a.getSize() * 8) < 0.000001) {
	// container.reserve(this.actualsizeinwords << 1);
	// xorVerbatimCompress(a, container);
	// } else {
	// container.reserve(this.actualsizeinwords);
	// xorVerbatim(a, container);
	// }
	// } else if (this.verbatim || a.verbatim) {
	// xorHybrid(a, container);
	// } else {
	// container.reserve(this.actualsizeinwords + a.actualsizeinwords);
	// xorToContainer(a, container);
	// }
	// return container;
	// }

  /**
   * Computes a new compressed bitmap containing the bitwise XOR values of the
   * current bitmap with some other bitmap.
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @param container
   *          where we store the result
   */
  public void xorToContainer(final HybridBitmap a, final BitmapStorage container) {
    final EWAHIterator i = a.getEWAHIterator();
    final EWAHIterator j = getEWAHIterator();
    final IteratingBufferedRunningLengthWord rlwi = new IteratingBufferedRunningLengthWord(i);
    final IteratingBufferedRunningLengthWord rlwj = new IteratingBufferedRunningLengthWord(j);
    while ((rlwi.size()>0) && (rlwj.size()>0)) {
      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
        final boolean i_is_prey = rlwi.getRunningLength() < rlwj
          .getRunningLength();
        final IteratingBufferedRunningLengthWord prey = i_is_prey ? rlwi : rlwj;
        final IteratingBufferedRunningLengthWord predator = i_is_prey ? rlwj
          : rlwi;
        if (predator.getRunningBit() == false) {
          long index = prey.discharge(container, predator.getRunningLength()); 
          container.addStreamOfEmptyWords(false, predator.getRunningLength()
            - index);
          predator.discardFirstWords(predator.getRunningLength());
        } else {
          long index = prey.dischargeNegated(container, predator.getRunningLength()); 
          container.addStreamOfEmptyWords(true, predator.getRunningLength()
            - index);
          predator.discardFirstWords(predator.getRunningLength());
        }
      }
      final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(),
        rlwj.getNumberOfLiteralWords());
      if (nbre_literal > 0) {
        for (int k = 0; k < nbre_literal; ++k)
          container.add(rlwi.getLiteralWordAt(k) ^ rlwj.getLiteralWordAt(k));
        rlwi.discardFirstWords(nbre_literal);
        rlwj.discardFirstWords(nbre_literal);
      }
    }      
    final boolean i_remains = rlwi.size()>0;
    final IteratingBufferedRunningLengthWord remaining = i_remains ? rlwi : rlwj;
    remaining.discharge(container);
    container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
  }
  
  
 
  
  public void xorToContainerDecompress(final HybridBitmap a, final HybridBitmap container) {
	  container.reserve(((this.sizeinbits>>>6) +1));
	  container.verbatim=true;
	  container.actualsizeinwords=0;
	  final EWAHIterator i = a.getEWAHIterator();
	    final EWAHIterator j = getEWAHIterator();
	    final IteratingBufferedRunningLengthWord rlwi = new IteratingBufferedRunningLengthWord(i);
	    final IteratingBufferedRunningLengthWord rlwj = new IteratingBufferedRunningLengthWord(j);
	    while ((rlwi.size()>0) && (rlwj.size()>0)) {
	      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
	        final boolean i_is_prey = rlwi.getRunningLength() < rlwj
	          .getRunningLength();
	        final IteratingBufferedRunningLengthWord prey = i_is_prey ? rlwi : rlwj;
	        final IteratingBufferedRunningLengthWord predator = i_is_prey ? rlwj
	          : rlwi;
	        if (predator.getRunningBit() == false) {
	          long index = prey.dischargeDecompressed(container, predator.getRunningLength()); 
	          //container.addStreamOfEmptyWords(false, predator.getRunningLength()
	           // - index);
	        	Arrays.fill(container.buffer, container.actualsizeinwords, (int) (container.actualsizeinwords+predator.getRunningLength()-index), 0);
	        	container.actualsizeinwords+=predator.getRunningLength()-index;
	          predator.discardFirstWords(predator.getRunningLength());
	        } else {
	          long index = prey.dischargeNegatedDecompressed(container, predator.getRunningLength()); 
	         // container.addStreamOfEmptyWords(true, predator.getRunningLength()
	          //  - index);
	          Arrays.fill(container.buffer, container.actualsizeinwords, (int) (container.actualsizeinwords+predator.getRunningLength()-index), ~0L);
	          container.actualsizeinwords+=predator.getRunningLength()-index;
	          predator.discardFirstWords(predator.getRunningLength());
	        }
	      }
	      final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(),
	        rlwj.getNumberOfLiteralWords());
	      if (nbre_literal > 0) {
	        for (int k = 0; k < nbre_literal; ++k){
	        	container.buffer[container.actualsizeinwords]=(rlwi.getLiteralWordAt(k) ^ rlwj.getLiteralWordAt(k));
		          container.actualsizeinwords++;
	          }
	        rlwi.discardFirstWords(nbre_literal);
	        rlwj.discardFirstWords(nbre_literal);
	      }
	    }
	    container.sizeinbits=Math.max(sizeInBits(), a.sizeInBits());
	    //System.out.println("container size: "+container.actualsizeinwords);
	   // final boolean i_remains = rlwi.size()>0;
	   // final IteratingBufferedRunningLengthWord remaining = i_remains ? rlwi : rlwj;
	   // remaining.discharge(container);
	  //  container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
	  }

  /**
   * Returns the cardinality of the result of a bitwise XOR of the values of the
   * current bitmap with some other bitmap. Avoids needing to allocate an
   * intermediate bitmap to hold the result of the OR.
   * 
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @return the cardinality
   */
  public int xorCardinality(final HybridBitmap a) {
    final BitCounter counter = new BitCounter();
    xorToContainer(a, counter);
    return counter.getCount();
  }

  /**
   * For internal use. Computes the bitwise and of the provided bitmaps and
   * stores the result in the container.
   * 
   * @param container
   *          where the result is stored
   * @param bitmaps
   *          bitmaps to AND
   * @since 0.4.3
   */
  public static void andWithContainer(final BitmapStorage container,
    final HybridBitmap... bitmaps) {
    if (bitmaps.length == 2) {
      // should be more efficient
      bitmaps[0].andToContainer(bitmaps[1], container);
      return;
    }

    // Sort the bitmaps in ascending order by sizeinbits. When we exhaust the
    // first bitmap the rest
    // of the result is zeros.
    final HybridBitmap[] sortedBitmaps = bitmaps.clone();
    Arrays.sort(sortedBitmaps, new Comparator<HybridBitmap>() {
      public int compare(HybridBitmap a, HybridBitmap b) {
        return a.sizeinbits < b.sizeinbits ? -1
          : a.sizeinbits == b.sizeinbits ? 0 : 1;
      }
    });

    int maxSize = sortedBitmaps[sortedBitmaps.length - 1].sizeinbits;

    final IteratingBufferedRunningLengthWord[] rlws = new IteratingBufferedRunningLengthWord[bitmaps.length];
    for (int i = 0; i < sortedBitmaps.length; i++) {
      EWAHIterator iterator = sortedBitmaps[i].getEWAHIterator();
      if (iterator.hasNext()) {
        rlws[i] = new IteratingBufferedRunningLengthWord(iterator);
      } else {
        // this never happens...
        if (maxSize > 0) {
          extendEmptyBits(container, 0, maxSize);
        }
        container.setSizeInBits(maxSize);
        return;
      }
    }

    while (true) {
      long maxZeroRl = 0;
      long minOneRl = Long.MAX_VALUE;
      long minSize = Long.MAX_VALUE;
      int numEmptyRl = 0;

      
      for (IteratingBufferedRunningLengthWord rlw : rlws) {
    	  
        long size = rlw.size();
        minSize = Math.min(minSize, size);

        if (!rlw.getRunningBit()) {
          long rl = rlw.getRunningLength();
          maxZeroRl = Math.max(maxZeroRl, rl);
          minOneRl = 0;
          if (rl == 0 && size > 0) {
            numEmptyRl++;
          }
        } else {
          long rl = rlw.getRunningLength();
          minOneRl = Math.min(minOneRl, rl);
          if (rl == 0 && size > 0) {
            numEmptyRl++;
          }
        }
      }

      if (minSize == 0) {
        extendEmptyBits(container, sortedBitmaps[0].sizeinbits, maxSize);
        break;
      }
      if (maxZeroRl > 0) {
        container.addStreamOfEmptyWords(false, maxZeroRl);
        for (IteratingBufferedRunningLengthWord rlw : rlws) {
          rlw.discardFirstWords(maxZeroRl);
        }
      } else if (minOneRl > 0) {
        container.addStreamOfEmptyWords(true, minOneRl);
        for (IteratingBufferedRunningLengthWord rlw : rlws) {
          rlw.discardFirstWords(minOneRl);
        }
      } else {
        int index = 0;

        if (numEmptyRl == 1) {
          // if one rlw has literal words to process and the rest have a run of
          // 1's we can write them out here
          IteratingBufferedRunningLengthWord emptyRl = null;
          long minNonEmptyRl = Long.MAX_VALUE;
          for (IteratingBufferedRunningLengthWord rlw : rlws) {
            long rl = rlw.getRunningLength();
            if (rl == 0) {
              assert emptyRl == null;
              emptyRl = rlw;
            } else {
              minNonEmptyRl = Math.min(minNonEmptyRl, rl);
            }
          }
          long wordsToWrite = minNonEmptyRl > minSize ? minSize : minNonEmptyRl;
          if (emptyRl != null)
            emptyRl.writeLiteralWords((int) wordsToWrite, container);
          index += wordsToWrite;
        }

        while (index < minSize) {
          long word = ~0l;
          for (IteratingBufferedRunningLengthWord rlw : rlws) {
            if (rlw.getRunningLength() <= index) {
              word &= rlw.getLiteralWordAt(index - (int) rlw.getRunningLength());
            }
          }
          container.add(word);
          index++;
        }
        for (IteratingBufferedRunningLengthWord rlw : rlws) {
          rlw.discardFirstWords(minSize);
        }
      }
    }
    container.setSizeInBits(maxSize);
  }

  /**
   * Returns a new compressed bitmap containing the bitwise AND values of the
   * provided bitmaps.
   * 
   * It may or may not be faster than doing the aggregation two-by-two (A.and(B).and(C)).
   * 
   * @since 0.4.3
   * @param bitmaps
   *          bitmaps to AND together
   * @return result of the AND
   */
  public static HybridBitmap and(final HybridBitmap... bitmaps) {
    final HybridBitmap container = new HybridBitmap();
    int largestSize = 0;
    for (HybridBitmap bitmap : bitmaps) {
      largestSize = Math.max(bitmap.actualsizeinwords, largestSize);
    }
    container.reserve((int) (largestSize * 1.5));
    andWithContainer(container, bitmaps);
    return container;
  }

  /**
   * Returns the cardinality of the result of a bitwise AND of the values of the
   * provided bitmaps. Avoids needing to allocate an intermediate bitmap to hold
   * the result of the AND.
   * 
   * @since 0.4.3
   * @param bitmaps
   *          bitmaps to AND
   * @return the cardinality
   */
  public static int andCardinality(final HybridBitmap... bitmaps) {
    final BitCounter counter = new BitCounter();
    andWithContainer(counter, bitmaps);
    return counter.getCount();
  }
  
  /**
   * Return a bitmap with the bit set to true at the given
   * positions. The positions should be given in sorted order.
   * 
   * (This is a convenience method.)
   * 
   * @since 0.4.5
   * @param setbits list of set bit positions
   * @return the bitmap
   */
  public static HybridBitmap bitmapOf(int ... setbits) {
    HybridBitmap a = new HybridBitmap();
    for (int k : setbits)
      a.set(k);
    return a;
  }




  /**
   * For internal use. This simply adds a stream of words made of zeroes so that
   * we pad to the desired size.
   * 
   * @param storage
   *          bitmap to extend
   * @param currentSize
   *          current size (in bits)
   * @param newSize
   *          new desired size (in bits)
   * @since 0.4.3
   */
  private static void extendEmptyBits(final BitmapStorage storage,
    final int currentSize, final int newSize) {
    final int currentLeftover = currentSize % wordinbits;
    final int finalLeftover = newSize % wordinbits;
    storage.addStreamOfEmptyWords(false, (newSize / wordinbits) - currentSize
      / wordinbits + (finalLeftover != 0 ? 1 : 0)
      + (currentLeftover != 0 ? -1 : 0));
  }

  /**
   * For internal use. Computes the bitwise or of the provided bitmaps and
   * stores the result in the container.
   * 
   * @since 0.4.0
   */
  public static void orWithContainer(final BitmapStorage container,
    final HybridBitmap... bitmaps) {
    if (bitmaps.length == 2) {
      // should be more efficient
      bitmaps[0].orToContainer(bitmaps[1], container);
      return;
    }

    // Sort the bitmaps in descending order by sizeinbits. We will exhaust the
    // sorted bitmaps from right to left.
    final HybridBitmap[] sortedBitmaps = bitmaps.clone();
    Arrays.sort(sortedBitmaps, new Comparator<HybridBitmap>() {
      public int compare(HybridBitmap a, HybridBitmap b) {
        return a.sizeinbits < b.sizeinbits ? 1
          : a.sizeinbits == b.sizeinbits ? 0 : -1;
      }
    });

    final IteratingBufferedRunningLengthWord[] rlws = new IteratingBufferedRunningLengthWord[bitmaps.length];
    int maxAvailablePos = 0;
    for (HybridBitmap bitmap : sortedBitmaps) {
      EWAHIterator iterator = bitmap.getEWAHIterator();
      if (iterator.hasNext()) {
        rlws[maxAvailablePos++] = new IteratingBufferedRunningLengthWord(
          iterator);
      }
    }

    if (maxAvailablePos == 0) { // this never happens...
      container.setSizeInBits(0);
      return;
    }

    int maxSize = sortedBitmaps[0].sizeinbits;

    while (true) {
      long maxOneRl = 0;
      long minZeroRl = Long.MAX_VALUE;
      long minSize = Long.MAX_VALUE;
      int numEmptyRl = 0;
      for (int i = 0; i < maxAvailablePos; i++) {
        IteratingBufferedRunningLengthWord rlw = rlws[i];
        long size = rlw.size();
        if (size == 0) {
          maxAvailablePos = i;
          break;
        }
        minSize = Math.min(minSize, size);

        if (rlw.getRunningBit()) {
          long rl = rlw.getRunningLength();
          maxOneRl = Math.max(maxOneRl, rl);
          minZeroRl = 0;
          if (rl == 0 && size > 0) {
            numEmptyRl++;
          }
        } else {
          long rl = rlw.getRunningLength();
          minZeroRl = Math.min(minZeroRl, rl);
          if (rl == 0 && size > 0) {
            numEmptyRl++;
          }
        }
      }

      if (maxAvailablePos == 0) {
        break;
      } else if (maxAvailablePos == 1) {
        // only one bitmap is left so just write the rest of it out
        rlws[0].discharge(container);
        break;
      }

      if (maxOneRl > 0) {
        container.addStreamOfEmptyWords(true, maxOneRl);
        for (int i = 0; i < maxAvailablePos; i++) {
          IteratingBufferedRunningLengthWord rlw = rlws[i];
          rlw.discardFirstWords(maxOneRl);
        }
      } else if (minZeroRl > 0) {
        container.addStreamOfEmptyWords(false, minZeroRl);
        for (int i = 0; i < maxAvailablePos; i++) {
          IteratingBufferedRunningLengthWord rlw = rlws[i];
          rlw.discardFirstWords(minZeroRl);
        }
      } else {
        int index = 0;

        if (numEmptyRl == 1) {
          // if one rlw has literal words to process and the rest have a run of
          // 0's we can write them out here
          IteratingBufferedRunningLengthWord emptyRl = null;
          long minNonEmptyRl = Long.MAX_VALUE;
          for (int i = 0; i < maxAvailablePos; i++) {
            IteratingBufferedRunningLengthWord rlw = rlws[i];
            long rl = rlw.getRunningLength();
            if (rl == 0) {
              assert emptyRl == null;
              emptyRl = rlw;
            } else {
              minNonEmptyRl = Math.min(minNonEmptyRl, rl);
            }
          }
          long wordsToWrite = minNonEmptyRl > minSize ? minSize : minNonEmptyRl;
          if (emptyRl != null)
            emptyRl.writeLiteralWords((int) wordsToWrite, container);
          index += wordsToWrite;
        }

        while (index < minSize) {
          long word = 0;
          for (int i = 0; i < maxAvailablePos; i++) {
            IteratingBufferedRunningLengthWord rlw = rlws[i];
            if (rlw.getRunningLength() <= index) {
              word |= rlw.getLiteralWordAt(index - (int) rlw.getRunningLength());
            }
          }
          container.add(word);
          index++;
        }
        for (int i = 0; i < maxAvailablePos; i++) {
          IteratingBufferedRunningLengthWord rlw = rlws[i];
          rlw.discardFirstWords(minSize);
        }
      }
    }
    container.setSizeInBits(maxSize);
  }

  /**
   * Returns a new compressed bitmap containing the bitwise OR values of the
   * provided bitmaps. This is typically faster than doing the aggregation
   * two-by-two (A.or(B).or(C).or(D)).
   * 
   * @since 0.4.0
   * @param bitmaps
   *          bitmaps to OR together
   * @return result of the OR
   */
  public static HybridBitmap or(final HybridBitmap... bitmaps) {
    final HybridBitmap container = new HybridBitmap();
    int largestSize = 0;
    for (HybridBitmap bitmap : bitmaps) {
      largestSize = Math.max(bitmap.actualsizeinwords, largestSize);
    }
    container.reserve((int) (largestSize * 1.5));
    orWithContainer(container, bitmaps);
    return container;
  }

  /**
   * Returns the cardinality of the result of a bitwise OR of the values of the
   * provided bitmaps. Avoids needing to allocate an intermediate bitmap to hold
   * the result of the OR.
   * 
   * @since 0.4.0
   * @param bitmaps
   *          bitmaps to OR
   * @return the cardinality
   */
  public static int orCardinality(final HybridBitmap... bitmaps) {
    final BitCounter counter = new BitCounter();
    orWithContainer(counter, bitmaps);
    return counter.getCount();
  }
  
  public double getDensity(){
	  return this.density;
  }

  /** The actual size in words. */
  public int actualsizeinwords = 1;

	public double density = 0;
	
	/**
	 * Threshold parameters for hybrid
	 */
	private double andThreshold =  0.0005;
	private double orThreshold = 0.001;


	public boolean verbatim = false;

  /** The buffer (array of 64-bit words) */
  public long buffer[] = null;

  /** The current (last) running length word. */
  RunningLengthWord rlw = null;

  /** sizeinbits: number of bits in the (uncompressed) bitmap. */
  int sizeinbits = 0;
  int age=0;

  /**
   * The Constant defaultbuffersize: default memory allocation when the object
   * is constructed.
   */
  static final int defaultbuffersize = 4;

  /** optimization option **/
  public static final boolean usetrailingzeros = true;

  /** The Constant wordinbits represents the number of bits in a long. */
  public static final int wordinbits = 64;

  
	public int getSize() {

	return this.sizeInBytes();
}

	public void concatenate(HybridBitmap a) {
		
		if (this.verbatim && a.verbatim){
			this.buffer = ArrayUtils.addAll(this.buffer, a.buffer);					
		}else if(this.verbatim){
			long[] firstWord = new long[]{new Long(this.buffer.length)<<33}; //first word indicates the number of following literals			
			this.buffer = ArrayUtils.addAll(firstWord, this.buffer);
			this.buffer = ArrayUtils.addAll(this.buffer, a.buffer);
			this.verbatim=false;						
		}else if (a.verbatim){
		    this.buffer = ArrayUtils.add(this.buffer, new Long(a.buffer.length) <<33);
		    this.buffer = ArrayUtils.addAll(this.buffer, a.buffer);		    		
		}else{
			this.buffer = ArrayUtils.addAll(this.buffer, a.buffer);
		}
		this.actualsizeinwords = this.buffer.length;
		this.density = (this.density*this.sizeinbits+a.density*a.sizeinbits)/(this.sizeinbits+a.sizeinbits);
		this.sizeinbits = this.sizeinbits+a.sizeinbits;	
		
	}


}

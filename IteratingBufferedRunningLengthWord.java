package hybridewah;

import java.util.Arrays;

/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc. and Veronika Zenz
 * Modified by Gheorghi Guzun to support operation of compressed and verbatim bitmaps.
 * Licensed under APL 2.0.
 */
/**
 * Mostly for internal use. Similar to BufferedRunningLengthWord, but automatically
 * advances to the next BufferedRunningLengthWord as words are discarded.
 *
 * @since 0.4.0
 * @author David McIntosh
 */
public final class IteratingBufferedRunningLengthWord {
  /**
   * Instantiates a new iterating buffered running length word.
   *
   * @param iterator iterator
   */
  public IteratingBufferedRunningLengthWord(final EWAHIterator iterator) {
    this.iterator = iterator;
    this.brlw = new BufferedRunningLengthWord(this.iterator.next());
    this.literalWordStartPosition = this.iterator.literalWords() + this.brlw.literalwordoffset;
    this.buffer = this.iterator.buffer();
  }
  
  /**
   * Instantiates a new iterating buffered running length word.
   *
   * @param iterator iterator
   */  
  public IteratingBufferedRunningLengthWord(final HybridBitmap bitmap) {
    this(EWAHIterator.getEWAHIterator(bitmap));
  }
  
  /**
   * Discard first words, iterating to the next running length word if needed.
   *
   * @param x the x
   */
  public void discardFirstWords(long x) {
    while (x > 0) {
      if (this.brlw.RunningLength > x) {
        this.brlw.RunningLength -= x;
        return;
      }
      x -= this.brlw.RunningLength;
      this.brlw.RunningLength = 0;
      long toDiscard = x > this.brlw.NumberOfLiteralWords ? this.brlw.NumberOfLiteralWords : x;
    
      this.literalWordStartPosition += toDiscard;
      this.brlw.NumberOfLiteralWords -= toDiscard;
      x -= toDiscard;
      if ((x > 0) || (this.brlw.size() == 0)) {
        if (!this.iterator.hasNext()) {
          break;
        }
        this.brlw.reset(this.iterator.next());
        this.literalWordStartPosition = this.iterator.literalWords() + this.brlw.literalwordoffset;
      }
    }
  }

  /**
   * Write out up to max words, returns how many were written
   * @param container target for writes
   * @param max maximal number of writes
   * @return how many written
   */
  public long discharge(BitmapStorage container, long max) {
    long index = 0;
    while ((index < max) && (size() > 0)) {
      // first run
      long pl = getRunningLength();
      if (index + pl > max) {
        pl = max - index;
      }
      container.addStreamOfEmptyWords(getRunningBit(), pl);
      index += pl;
      int pd = getNumberOfLiteralWords();
      if (pd + index > max) {
        pd = (int) (max - index);
      }
      writeLiteralWords(pd, container);
      discardFirstWords(pl+pd);
      index += pd;
    }
    return index;
  }
  
  
  public long dischargeDecompressed(HybridBitmap container, long max) {
	    long index = 0;
	    while ((index < max) && (size() > 0)) {
	      // first run
	      long pl = getRunningLength();
	      if (index + pl > max) {
	        pl = max - index;
	      }
	      Arrays.fill(container.buffer, container.actualsizeinwords, (int) (container.actualsizeinwords+pl), getRunningBit()?~0L:0);
	      container.actualsizeinwords+=pl;
	    //  container.addStreamOfEmptyWords(getRunningBit(), pl);
	      index += pl;
	      int pd = getNumberOfLiteralWords();
	      if (pd + index > max) {
	        pd = (int) (max - index);
	      }
	      for(int i=0; i< pd; i++){
	    	  container.buffer[container.actualsizeinwords+i]=this.buffer[this.literalWordStartPosition+i];	    	  
	      }
	      container.actualsizeinwords+=pd;
	      discardFirstWords(pl+pd);
	      index += pd;
	    }
	    return index;
	  }

  /**
   * Write out up to max words (negated), returns how many were written
   * @param container target for writes
   * @param max maximal number of writes
   * @return how many written
   */
  public long dischargeNegated(BitmapStorage container, long max) {
    long index = 0;
    while ((index < max) && (size() > 0)) {
      // first run
      long pl = getRunningLength();
      if (index + pl > max) {
        pl = max - index;
      }
     
      container.addStreamOfEmptyWords(!getRunningBit(), pl);
      discardFirstWords(pl);
      index += pl;
      int pd = getNumberOfLiteralWords();
      if (pd + index > max) {
        pd = (int) (max - index);
      }
      writeNegatedLiteralWords(pd, container);
      discardFirstWords(pd);
      index += pd;
    }
    return index;
  }
  
  public long dischargeNegatedDecompressed(HybridBitmap container, long max) {
	    long index = 0;
	    while ((index < max) && (size() > 0)) {
	      // first run
	      long pl = getRunningLength();
	      if (index + pl > max) {
	        pl = max - index;
	      }
	      Arrays.fill(container.buffer, container.actualsizeinwords, (int) (container.actualsizeinwords+pl), getRunningBit()?0:~0L);
	      container.actualsizeinwords+=pl;
	    //  container.addStreamOfEmptyWords(getRunningBit(), pl);
	      index += pl;
	      int pd = getNumberOfLiteralWords();
	      if (pd + index > max) {
	        pd = (int) (max - index);
	      }
	      for(int i=0; i< pd; i++){
	    	  container.buffer[container.actualsizeinwords+i]=~this.buffer[this.literalWordStartPosition+i];	    	  
	      }
	      container.actualsizeinwords+=pd;
	      discardFirstWords(pl+pd);
	      index += pd;
	    }
	    return index;
	  }


  /**
   * Write out the remain words, transforming them to zeroes.
   * @param container target for writes
   */
  public void dischargeAsEmpty(BitmapStorage container) {
    while(size()>0) {
      container.addStreamOfEmptyWords(false, size());
      discardFirstWords(size());
    }
  }
  
  

  /**
   * Write out the remaining words
   * @param container target for writes
   */
  public void discharge(BitmapStorage container) {
    this.brlw.literalwordoffset = this.literalWordStartPosition - this.iterator.literalWords();
    discharge(this.brlw, this.iterator, container);
  }

  /**
   * Get the nth literal word for the current running length word 
   * @param index zero based index
   * @return the literal word
   */
  public long getLiteralWordAt(int index) {
    return this.buffer[this.literalWordStartPosition + index];
  }

  /**
   * Gets the number of literal words for the current running length word.
   *
   * @return the number of literal words
   */
  public int getNumberOfLiteralWords() {
    return this.brlw.NumberOfLiteralWords;
  }

  /**
   * Gets the running bit.
   *
   * @return the running bit
   */
  public boolean getRunningBit() {
    return this.brlw.RunningBit;
  }
  
  /**
   * Gets the running length.
   *
   * @return the running length
   */
  public long getRunningLength() {
    return this.brlw.RunningLength;
  }
  
  /**
   * Size in uncompressed words of the current running length word.
   *
   * @return the long
   */
  public long size() {
    return this.brlw.size();
  }
  
  /**
   * write the first N literal words to the target bitmap.  Does not discard the words or perform iteration.
   * @param numWords
   * @param container
   */
  public void writeLiteralWords(int numWords, BitmapStorage container) {
    container.addStreamOfLiteralWords(this.buffer, this.literalWordStartPosition, numWords);
  }

  /**
   * write the first N literal words (negated) to the target bitmap.  Does not discard the words or perform iteration.
   * @param numWords
   * @param container
   */
  public void writeNegatedLiteralWords(int numWords, BitmapStorage container) {
    container.addStreamOfNegatedLiteralWords(this.buffer, this.literalWordStartPosition, numWords);
  }
  
  /**
   * For internal use.
   * 
   * @param initialWord
   *          the initial word
   * @param iterator
   *          the iterator
   * @param container
   *          the container
   */
  private static void discharge(final BufferedRunningLengthWord initialWord,
    final EWAHIterator iterator, final BitmapStorage container) {
    BufferedRunningLengthWord runningLengthWord = initialWord;
    for (;;) {
      final long runningLength = runningLengthWord.getRunningLength();
      container.addStreamOfEmptyWords(runningLengthWord.getRunningBit(),
        runningLength);
      container.addStreamOfLiteralWords(iterator.buffer(), iterator.literalWords()
        + runningLengthWord.literalwordoffset,
        runningLengthWord.getNumberOfLiteralWords());
      if (!iterator.hasNext())
        break;
      runningLengthWord = new BufferedRunningLengthWord(iterator.next());
    }
  }
  
  private BufferedRunningLengthWord brlw;
  private long[] buffer;
  private int literalWordStartPosition;
  private EWAHIterator iterator;
}

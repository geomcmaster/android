package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.WeakHashMap;

/**
 * The default implementation of the {@link BooksStatusCacheType} interface.
 */

public final class BooksStatusCache extends Observable
  implements BooksStatusCacheType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(BooksStatusCache.class));
  }

  private final Map<BookID, BookStatusType>            status;
  private final Map<BookID, FeedEntryType>             entries;

  private BooksStatusCache()
  {
    this.status = new HashMap<BookID, BookStatusType>(32);
    this.entries = new WeakHashMap<BookID, FeedEntryType>(32);
  }

  /**
   * @return A new status cache
   */

  public static BooksStatusCacheType newStatusCache()
  {
    return new BooksStatusCache();
  }

  @Override public void booksObservableAddObserver(
    final Observer o)
  {
    this.addObserver(o);
  }

  @Override public void booksObservableDeleteAllObservers()
  {
    this.deleteObservers();
  }

  @Override public void booksObservableDeleteObserver(
    final Observer o)
  {
    this.deleteObserver(o);
  }

  @Override public synchronized void booksObservableNotify(
    final BookID in_id)
  {
    this.broadcast(NullCheck.notNull(in_id));
  }

  @Override public synchronized void booksStatusClearAll()
  {
    this.status.clear();
  }

  @Override public synchronized OptionType<BookStatusType> booksStatusGet(
    final BookID id)
  {
    NullCheck.notNull(id);
    if (this.status.containsKey(id)) {
      return Option.some(NullCheck.notNull(this.status.get(id)));
    }
    return Option.none();
  }

  @Override
  public synchronized OptionType<FeedEntryType> booksRevocationFeedEntryGet(
    final BookID id)
  {
    NullCheck.notNull(id);
    if (this.entries.containsKey(id)) {
      return Option.some(NullCheck.notNull(this.entries.get(id)));
    }
    return Option.none();
  }

  @Override public synchronized void booksRevocationFeedEntryUpdate(
    final FeedEntryType e)
  {
    NullCheck.notNull(e);

    final BookID id = e.getBookID();
    BooksStatusCache.LOG.debug("entry update {} → {}", id, e);
    this.entries.put(id, e);
    this.broadcast(id);
  }

  @Override public synchronized void booksStatusUpdate(
    final BookStatusType s)
  {
    this.put(NullCheck.notNull(s));
  }

  @Override public synchronized void booksStatusUpdateIfMoreImportant(
    final BookStatusType update)
  {
    final BookID id = NullCheck.notNull(update).getID();
    if (this.status.containsKey(id)) {
      final BookStatusType current = NullCheck.notNull(this.status.get(id));
      final BookStatusPriorityOrdering current_p = current.getPriority();
      final BookStatusPriorityOrdering update_p = update.getPriority();

      if (current_p.getPriority() <= update_p.getPriority()) {
        BooksStatusCache.LOG.debug(
          "current {} <= {}, updating", current, update);
        this.put(update);
        return;
      }

      if (current_p.getPriority() > update_p.getPriority()) {
        BooksStatusCache.LOG.debug(
          "current {} > {}, not updating", current, update);
        return;
      }

    } else {
      this.booksStatusUpdate(update);
    }
  }

  @Override public synchronized void booksStatusClearFor(final BookID book_id)
  {
    BooksStatusCache.LOG.debug("clear {}", book_id);
    this.status.remove(book_id);
    this.broadcast(book_id);
  }

  private void broadcast(
    final BookID id)
  {
    this.setChanged();
    this.notifyObservers(id);
  }

  private <T extends BookStatusType> T put(
    final T s)
  {
    final BookID id = s.getID();
    BooksStatusCache.LOG.debug("put {}", s);
    this.status.put(id, s);
    this.broadcast(id);
    return s;
  }
}

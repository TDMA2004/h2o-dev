package water;

/**
 * Distributed Key/Value Store
 *
 * This class handles the distribution pattern.
 *
 * @author <a href="mailto:cliffc@0xdata.com"></a>
 * @version 1.0
 */
public abstract class DKV {
  // This put is a top-level user-update, and not a reflected or retried
  // update.  i.e., The User has initiated a change against the K/V store.

  // Put an Iced, by wrapping it in a Value
  static public Value put( Key key, Iced v ) { return put(key,new Value(key,v)); }
  static public Value put( Key key, Iced v, Futures fs ) { return put(key,new Value(key,v),fs); }
  static public Value put( Key key, Iced v, Futures fs,boolean donCache ) {
    return put(key,new Value(key,v),fs,donCache);
  }

  // Put a Value, blocking by default.
  static public Value put( Key key, Value val ) { 
    Futures fs = new Futures(); 
    Value old = put(key,val,fs);
    fs.blockForPending();
    return old;
  }
  static public Value put( Key key, Value val, Futures fs ) { return put(key,val,fs,false);}
  static public Value put( Key key, Value val, Futures fs, boolean dontCache ) {
    assert key != null;
    assert val==null || val._key == key:"non-matching keys " + key.toString() + " != " + val._key.toString();
    while( true ) {
      Value old = H2O.raw_get(key); // Raw-get: do not lazy-manifest if overwriting
      Value res = DputIfMatch(key,val,old,fs,dontCache);
      if( res == old ) return old; // PUT is globally visible now?
      if( val != null && val._key != key ) key = val._key;
    }
  }

  // Remove this Key, blocking by default
  static public Value remove( Key key ) { return put(key,null); }
  static public Value remove( Key key, Futures fs ) { return put(key,null,fs); }

  static public Value DputIfMatch( Key key, Value val, Value old, Futures fs) { return DputIfMatch(key, val, old, fs, false);  }

  // Do a PUT, and on success trigger replication.  Returns the prior Value on
  // either success or fail.  If a Futures is passed in, it can be used to
  // block until the PUT completes cluster-wide.
  static public Value DputIfMatch( Key key, Value val, Value old, Futures fs, boolean dontCache ) {
    // For debugging where keys are created from
//    try { System.err.flush(); System.err.println(key); Thread.dumpStack(); System.err.flush(); } catch (Throwable t) {}

    // First: I must block repeated remote PUTs to the same Key until all prior
    // ones complete - the home node needs to see these PUTs in order.
    // Repeated PUTs on the home node are already ordered.
    if( old != null && !key.home() ) old.startRemotePut();

    // local update first, since this is a weak update
    Value res = H2O.putIfMatch(key,val,old);
    if( res != old )            // Failed?
      return res;               // Return fail value

    // Check for trivial success: no need to invalidate remotes if the new
    // value equals the old.
    if( old != null && old == val ) return old; // Trivial success?
    if( old != null && val != null && val.equals(old) )
      return old;               // Less trivial success, but no network i/o

    // Before we start doing distributed writes... block until the cloud
    // stablizes.  After we start doing distrubuted writes, it is an error to
    // change cloud shape - the distributed writes will be in the wrong place.
    Paxos.lockCloud();

    // The 'D' part of DputIfMatch: do Distribution.
    // If PUT is on     HOME, invalidate remote caches
    // If PUT is on non-HOME, replicate/push to HOME
    if( key.home() ) {          // On     HOME?
      if( old != null ) old.lockAndInvalidate(H2O.SELF,fs);
    } else {                    // On non-HOME?
      // Start a write, but do not block for it
      TaskPutKey.put(key.home_node(),key,val,fs, dontCache);
    }
    return old;
  }

  // Stall until all existing writes have completed.
  // Used to order successive writes.
  static public void write_barrier() {
    for( H2ONode h2o : H2O.CLOUD._memary )
      for( RPC rpc : h2o.tasks() )
        if( rpc._dt instanceof TaskPutKey || rpc._dt instanceof Atomic )
          rpc.get();
  }

  // User-Weak-Get a Key from the distributed cloud.
  static public Value get    ( Key key ) { return get(key,true ); }
  static public void prefetch( Key key ) {        get(key,false); }
  static public Value get    ( String key_name)  { return get(Key.make(key_name),true ); }
  static public void prefetch( String key_name ) {        get(Key.make(key_name),false); }

  static private Value get( Key key, boolean blocking ) {
    // Read the Cloud once per put-attempt, to keep a consistent snapshot.
    H2O cloud = H2O.CLOUD;
    Value val = H2O.get(key);
    // Hit in local cache?
    if( val != null ) {
      if( val.rawMem() != null || val.rawPOJO() != null || val.isPersisted() ) return val;
      assert !key.home(); // Master must have *something*; we got nothing & need to fetch
    }

    // While in theory we could read from any replica, we always need to
    // inform the home-node that his copy has been Shared... in case it
    // changes and he needs to issue an invalidate.  For now, always and only
    // fetch from the Home node.
    H2ONode home = cloud._memary[key.home(cloud)];

    // If we missed in the cache AND we are the home node, then there is
    // no V for this K (or we have a disk failure).
    if( home == H2O.SELF ) return null;

    // Pending write to same key from this node?  Take that write instead.
    // Moral equivalent of "peeking into the cpu store buffer".  Can happen,
    // e.g., because a prior 'put' of a null (i.e. a remove) is still mid-
    // send to the remote, so the local get has missed above, but a remote
    // get still might 'win' because the remote 'remove' is still in-progress.
    for( RPC<?> rpc : home.tasks() ) {
      DTask dt = rpc._dt;       // Read once; racily changing
      if( dt instanceof TaskPutKey ) {
        assert rpc._target == home;
        TaskPutKey tpk = (TaskPutKey)dt;
        Key k = tpk._key;
        if( k != null && key.equals(k) )
          return tpk._xval;
      }
    }
    // Get data "the hard way"
    RPC<TaskGetKey> tgk = TaskGetKey.start(home,key);
    return blocking ? TaskGetKey.get(tgk) : null;
  }
}

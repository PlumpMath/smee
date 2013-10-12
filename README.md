# smee

An asynchronous hook/callback library for Clojure using core.async.

> 'Most of all,' Hook was saying passionately, 'I want their captain,
> Peter Pan. 'Twas he cut off my arm.' He brandished the hook
> threateningly. 'I've waited long to shake his hand with this. Oh, I'll
> tear him.'
>
> 'And yet,' said Smee, 'I have often heard you say that hook was worth
> a score of hands, for combing the hair and other homely uses.'
>
> 'Ay,' the captain answered, 'if I was a mother I would pray to have my
> children born with this instead of that,' and he cast a look of pride
> upon his iron hand and one of scorn upon the other.

-- Peter and Wendy, By James Matthew Barrie

## Installation

Clojars install via Leiningen coming soon.

## Usage

Say you're adding a user, someplace in your system, and you would like
other parts of your system to react to this.  You don't want any
return value, so an asynchronous fire-and-forget style is fine.

In the code you want to react to the user's creation:

```clj
(add-hook :user-created
          (fn [user-name]
            (do-something-contrived user-name)))
```

And in your user code:

```clj
(run-hook :user-created user-name)
```

```do-something-contrived``` will be called asynchronously.

You can define as many callbacks as you'd like and they will all be
called on a corresponding ```run-hook``` call.  They are called in a
non-deterministic order.  Don't write any code that expects other
hooks to have run beforehand.

### Limitations

It's worth pointing out that there is an underlying thread pool that
will limit the number of different hooks that can run concurrently.
This means that if either the throughput of functions running hooks is
too high, or if the hook functions themselves take too long then hook
processing will back up.  Also considering that args are kept in
memory until they are processed, memory usage could become a concern
if too much is queued up.

In general usage, Smee should be fine, but it is worth keeping the
above in mind.

## License

Copyright © 2013 David Leatherman

Distributed under the Eclipse Public License, the same as Clojure.

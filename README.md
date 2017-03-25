# Battle Arena

![Battle Arena gameplay](https://www.dropbox.com/s/cnowxhgtf55rzdx/battle-arena-0.gif?raw=1)

# Dependencies

- Unity 4.6
- Ruby

# Setting up the environment

## Downloading Battle Arena

```Bash
git clone git@github.com:mpereira/battle-arena.git
```

## Setting up Unity

1. Open Unity.
2. Click your way to `Edit > Project Settings > Player`
3. Make sure the `Run In Background*` checkbox under `Resolution and
   Presentation` is checked.
4. Make sure the `Api Compatibility Level` select under `Other Settings` is set
   to ".NET 2.0".
5. Click `File > Open Project`,  `Open Other...` and then select the
   Battle Arena git repository directory.

Now you should see "Starting REPL..." in the Unity Console and a "ClojureRepl"
window. Unity is ready to run some Clojure!

# Running Battle Arena

In the Battle Arena git repository directory:

```Bash
./load.sh
```

Now you should see a populated scene in the Unity window. If you press the play
button you should be able to click around to move the blue block.

# Interacting with the game

```Bash
$ ruby Assets/Arcadia/Editor/repl-client.rb
; Arcadia REPL
; Clojure 1.7.0-master-SNAPSHOT
; Unity 4.6.1f1 (d1db7a1b5196)
; Mono 2.6.5 (tarball)


user=>
```

TODO

## Author
   [Murilo Pereira](http://murilopereira.com)

## License
   [MIT](http://opensource.org/licenses/MIT)

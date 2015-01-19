# battle-arena

## Development

### Build compiler watcher

```Bash
$ lein cljsbuild auto
```

### Browser-connected ViM-REPL

1. Start an HTTP server

  ```Bash
  $ python -m SimpleHTTPServer 8080
  ```

2. Open a ClojureScript buffer in ViM

  ```Bash
  $ vim src/example/core.cljs
  ```

3. Create an nREPL session with the browser environment

  ```Vim
  :Piggieback (cljs.repl.browser/repl-env :port 9000)
  ```

4. Navigate to an HTML file that includes a ClojureScript file that contains the
   following line

  ```Clojure
  (repl/connect "http://localhost:9000/repl")
  ```

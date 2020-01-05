((clojure-mode (eval . (progn
                         (cider-mode -1)
                         (miracle-interaction-mode))))
 (miracle-mode (miracle-repl-prompt-format . "\n%s=> "))
 (nil (eval . (setq-local counsel-find-file-ignore-regexp
                          (regexp-opt '("Arcadia/Compiled"
                                        ".meta"
                                        ".asset"
                                        ".dll"))))))

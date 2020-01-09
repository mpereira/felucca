((clojure-mode (eval . (progn
                         (cider-mode -1)
                         (miracle-interaction-mode)
                         (setq-local dash-at-point-docset "unity3d"))))
 (miracle-mode (miracle-repl-prompt-format . "\n%s=> "))
 (nil (eval . (progn
                (setq-local counsel-find-file-ignore-regexp
                            (regexp-opt '("Arcadia/Compiled"
                                          ".meta"
                                          ".asset"
                                          ".dll")))))))

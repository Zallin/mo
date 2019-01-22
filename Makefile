.PHONY: test

repl:
	clj -A:dev -m "tools.repl" -p 3001

test:
	clj -A:dev -m "tools.test"

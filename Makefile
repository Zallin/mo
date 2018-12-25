.PHONY: test

repl:
	clj -A:dev -m "nrepl"

test:
	clj -A:dev -m "cognitect.test-runner" -d "test"


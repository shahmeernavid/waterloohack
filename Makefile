all: run
files:
	mkdir files
run: files
	./index.py
.PHONY: all run

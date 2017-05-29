.PHONY: clean

FILES ?= $(shell echo *.yml | sed "s/\.yml/\.json/g")

clean:
	rm -rf ${FILES}

build: clean ${FILES}

%.json:
	$(eval basename := $(shell echo $@ | cut -d '.' -f 1))
	@js-yaml ${basename}.yml > $@

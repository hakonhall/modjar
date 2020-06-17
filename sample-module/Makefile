DIR := ../src/test/resources/module-info
CLASS_FILE := $(DIR)/module-info.class
MODULE_FILE := target/classes/module-info.class

$(CLASS_FILE): $(MODULE_FILE) $(DIR)
	cp $< $@

$(DIR):
	mkdir $@

$(MODULE_FILE): src/module-info.java
	modulec -v 1.2.3 src

clean:
	rm -rf target

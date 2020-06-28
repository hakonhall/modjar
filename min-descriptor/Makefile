../src/test/resources/min-descriptor/module-info.class: module-info.class ../src/test/resources/min-descriptor
	cp $< $@

../src/test/resources/min-descriptor:
	mkdir -p $@

min.jar: module-info.class
	jar cf $@ $^

module-info.class: module-info.java
	javac $^

clean:
	rm -f module-info.class min.jar

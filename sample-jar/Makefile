sample.jar: classes/a/A.class
	jar -c -f $@ $^

classes/a/A.class: src/a/A.java
	javac -d classes src/a/A.java

clean:
	rm -rf classes

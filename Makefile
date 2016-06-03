PROJECT = 	prj1

VERBOSE=	
TARGET=		jar

$(TARGET):
		ant -emacs $(VERBOSE) $(TARGET)


clean:		
		ant -emacs $(VERBOSE) clean


submit:
		tar -cvzf $(PROJECT).tar.gz README build.xml Makefile \
                  `find src -name '*.java'`

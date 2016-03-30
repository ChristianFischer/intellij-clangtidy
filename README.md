# CLion-clang-tidy #

This plugin provides an user interface within CLion to easily use clang-tidy
on your project's sources.

### What is clang-tidy ###

clang-tidy is part of Clang, the C/C++/ObjectiveC compiler backend of LLVM.

When running clang-tidy, it performs a static code analysis to find some
common issues and code style violations and provides fixes which can be applied
to your code.

Some checks of clang-tidy, formerly known as clang-modernize, checks for
code constructs, which can be converted into modern C++11 syntax. For example
this includes:

* converting iterator based or index based for loops into range based
foreach loops
* adding the `override` keyword to overridden functions in subclasses
* replacing `NULL` macros with `nullptr`
* replacing function arguments to match the pass-by-value idiom to
gain advantage from new move semantics

To learn more about clang-tidy visit

* [clang-tidy documentation](http://clang.llvm.org/extra/clang-tidy/)
* [list of all clang-tidy checks](http://clang.llvm.org/extra/clang-tidy/checks/list.html)

### Setup ###

1. Download this plugin from IntelliJ plugin portal.
2. Visit [llvm.org](http://llvm.org) to download the latest version of llvm and clang
   or view instructions how to install them on your preferred linux distribution.
3. In CLion open your settings and go to Other settings / clang-tidy and enter the
   full path to your clang-tidy executable.

### Usage ###
1. Before using clang-tidy, you should commit your current working directory
   into VCS or backup your project, if clang-tidy produces errors in your code.
2. Right click on a single C/C++/ObjC file, opened editor or directory containing
   source files and select Refactor / clang-tidy.
3. In the configuration dialog select the checks you want to apply and click OK
   to start clang-tidy.
4. When finished, a list of all files containing issues will be displayed to you.
   Select the files you want to apply or click *merge* to view suggested fixes
   for a single selected file and apply them one by one.
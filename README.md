# Minstrel
A Javascript bridge for android that accepts all parameter types (object, functions, etc) and handles return values with the same flexibility

___

## Why?

Out of the box, Android's WebView visual component has many limitations when bridging Javascript to Java, mostly around the following:

- Interaction from Java->Javascript requires loadUrl("javascript:<a URI encoded javascript string>")
- Interaction from Javascript->Java only accepts Strings, Doubles, Integers and Booleans.  No Objects, Functions, etc are handled, resulting in a parameter of 'undefined'.  This is very limiting as Javascript devs often want to pass in callbacks, objects, etc.
- Calling a javascript function from Java does not allow for receiving a return value in earlier SDKs.  This is mitigated somewhat by the addition of evaluateJavascript, but even then, the return value can *only* be a string.

## Introduction

This library brings Android's webview/bridge support very much on par with iOS's JavascriptCore framework.  It allows for parameters to be passed to and from Javascript of any type.

ie: This now works when calling from Javascript back to the native app.  The app will then execute the callback (2nd parameter) upon completion.
```javascript
android.updateRecords({ name: "Herbert", age: 22 }, function (status) { updateWebUI(status); });
```

###### THG's Primary Contributors

Dr. Sneed (@bsneed)<br>
Steve Riggins (@steveriggins)<br>
Sam Grover (@samgrover)<br>
Angelo Di Paolo (@angelodipaolo)<br>
Cody Garvin (@migs647)<br>
Wes Ostler (@wesostler)<br>

## License

The MIT License (MIT)

Copyright (c) 2015 Walmart, TheHolyGrail, and other Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

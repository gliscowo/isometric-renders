package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.IsometricRenders;
import ca.weblite.objc.Client;
import ca.weblite.objc.Proxy;

public class MacOSClipboard {
    public static void copyFile (String path) {
        // Implementation taken from comp500's "ScreenshotToClipboard".

        // BEGIN LICENSED CODE

        // MIT License
        //
        // Copyright (c) 2018 comp500
        //
        // Permission is hereby granted, free of charge, to any person obtaining a copy
        // of this software and associated documentation files (the "Software"), to deal
        // in the Software without restriction, including without limitation the rights
        // to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
        // copies of the Software, and to permit persons to whom the Software is
        // furnished to do so, subject to the following conditions:
        //
        // The above copyright notice and this permission notice shall be included in all
        // copies or substantial portions of the Software.
        //
        // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
        // IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
        // FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
        // AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
        // LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
        // OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
        // SOFTWARE.

        // macOS requires some ugly hacks to get it to work, because it doesn't allow GLFW and AWT to load at the same time
        // See: https://github.com/MinecraftForge/MinecraftForge/pull/5591#issuecomment-470805491
        // Thanks to @juliand665 for writing and testing most of this code, I don't have a Mac!

        Client client = Client.getInstance();
        Proxy url = client.sendProxy("NSURL", "fileURLWithPath:", path);

        Proxy image = client.sendProxy("NSImage", "alloc");
        image.send("initWithContentsOfURL:", url);

        Proxy array = client.sendProxy("NSArray", "array");
        array = array.sendProxy("arrayByAddingObject:", image);

        Proxy pasteboard = client.sendProxy("NSPasteboard", "generalPasteboard");
        pasteboard.send("clearContents");
        boolean wasSuccessful = pasteboard.sendBoolean("writeObjects:", array);
        if (!wasSuccessful) {
            IsometricRenders.LOGGER.error("Failed to write image to pasteboard!");
        }

        // END LICENSED CODE
    }
}

/*
 * Hivemall: Hive scalable Machine Learning Library
 *
 * Copyright (C) 2015 Makoto YUI
 * Copyright (C) 2013-2015 National Institute of Advanced Industrial Science and Technology (AIST)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hivemall.tools.compress;

import hivemall.utils.compress.DeflateCodec;
import hivemall.utils.hadoop.HiveUtils;
import hivemall.utils.io.IOUtils;

import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.UDFType;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.StringObjectInspector;
import org.apache.hadoop.io.Text;

@Description(name = "deflate",
        value = "_FUNC_(TEXT data) - Returns compressed string by using Deflater")
@UDFType(deterministic = true, stateful = false)
public final class DeflateUDF extends GenericUDF {

    private StringObjectInspector stringOI;

    private transient DeflateCodec codec;

    @Override
    public ObjectInspector initialize(ObjectInspector[] argOIs) throws UDFArgumentException {
        if (argOIs.length != 1) {
            throw new UDFArgumentException("_FUNC_ takes exactly 1 argument");
        }
        this.stringOI = HiveUtils.asStringOI(argOIs[0]);

        return PrimitiveObjectInspectorFactory.writableStringObjectInspector;
    }

    @Override
    public Object evaluate(DeferredObject[] arguments) throws HiveException {
        if (codec == null) {
            this.codec = new DeflateCodec();
        }

        Object arg0 = arguments[0].get();
        if (arg0 == null) {
            return null;
        }
        Text text = stringOI.getPrimitiveWritableObject(arg0);
        byte[] original = text.getBytes();
        final int len = text.getLength();        
        final byte[] compressed;
        try {
            compressed = codec.compress(original, 0, len);
        } catch (IOException e) {
            throw new HiveException("Failed to compress", e);
        }
        original = null;
        Text ret = new Text(compressed);
        return ret;
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(codec);
        this.codec = null;
    }

    @Override
    public String getDisplayString(String[] children) {
        return "deflate(" + Arrays.toString(children) + ")";
    }

}

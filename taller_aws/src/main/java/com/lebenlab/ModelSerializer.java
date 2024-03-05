package com.lebenlab;

import com.lebenlab.core.FilePath;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import static com.lebenlab.ProcessArgException.error_reading_serialized_model;
import static java.nio.file.Files.newInputStream;

/**
 * User: pedro@didekin
 * Date: 30/03/2020
 * Time: 16:59
 */
public class ModelSerializer<T> {

    public final T model;

    public ModelSerializer(FilePath path)
    {
        this.model = read(path);
    }

    public ModelSerializer(T model)
    {
        this.model = model;
    }

    /**
     * Writes an object to a file and returns the path of file.
     */
    public void write(FilePath path)
    {
        try (OutputStream file = Files.newOutputStream(path.path());
             ObjectOutputStream out = new ObjectOutputStream(file)) {
            out.writeObject(model);
        } catch (IOException e) {
            throw new ProcessArgException(error_reading_serialized_model + path.path().toString());
        }
    }

    /**
     * Reads an object from a file.
     */
    public static <T> T read(FilePath path)
    {
        try (InputStream file = newInputStream(path.path());
             ObjectInputStream in = new ObjectInputStream(file)) {
            @SuppressWarnings("unchecked") T model = (T) in.readObject();
            return model;
        } catch (IOException | ClassNotFoundException e) {
            throw new ProcessArgException(ProcessArgException.error_writing_serialized_model + path.path().toString());
        }
    }
}

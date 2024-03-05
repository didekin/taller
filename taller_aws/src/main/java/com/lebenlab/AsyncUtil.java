package com.lebenlab;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * User: pedro@didekin
 * Date: 10/05/2021
 * Time: 15:53
 */
public final class AsyncUtil {
    public static ExecutorService executor(int numThreads){
        return Executors.newFixedThreadPool(numThreads);
    }
}

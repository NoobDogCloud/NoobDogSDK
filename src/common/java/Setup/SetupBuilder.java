package common.java.Setup;

import common.java.File.FileHelper;
import common.java.nLogger.nLogger;

import java.io.File;
import java.util.function.BooleanSupplier;

public class SetupBuilder {
    private static SetupBuilder instance;
    private final File locker;
    private final BooleanSupplier supplier;

    private SetupBuilder(BooleanSupplier supplier) {
        locker = new File("./setup.txt");
        this.supplier = supplier;
    }

    public static SetupBuilder getInstance(BooleanSupplier supplier) {
        if (instance != null) {
            return instance;
        }
        instance = new SetupBuilder(supplier);
        return instance;
    }

    public boolean install() {
        if (supplier == null || locker.exists()) {
            return false;
        }
        if (!FileHelper.createFile(locker.getAbsolutePath())) {
            nLogger.errorInfo("创建锁文件失败");
            return false;
        }
        return supplier.getAsBoolean();
    }

    public boolean reInstall() {
        locker.delete();
        return install();
    }
}

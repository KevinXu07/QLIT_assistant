package com.schedule.qlit;

/**
 * IUserService 的手写 Binder 实现。
 *
 * 原本是 IUserService.aidl 由 AIDL 编译器生成;AGP 9 的 KMP library 插件
 * (com.android.kotlin.multiplatform.library) 不再支持 aidl 编译,
 * 因此把等价的生成代码固化为 Java 源码。事务码与历史版本保持一致,
 * 升级时请勿重排/复用。
 *
 * CDP 桥接方法(TRANSACTION 5-8,微信 devtools 抓取)已随出行登记功能移除;
 * 剩余事务码保持旧编号以免与已发布版本的 UserService 握手错位。
 */
public interface IUserService extends android.os.IInterface {

    void destroy() throws android.os.RemoteException;

    void exit() throws android.os.RemoteException;

    /** 执行 shell 命令 */
    boolean execShell(String command) throws android.os.RemoteException;

    /** 设置 UID 防火墙规则 */
    boolean setUidFirewallRule(int uid, boolean block) throws android.os.RemoteException;

    /** 验证 UserService 是否在线 */
    String ping() throws android.os.RemoteException;

    abstract class Stub extends android.os.Binder implements IUserService {
        private static final String DESCRIPTOR = "com.schedule.qlit.IUserService";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IUserService asInterface(android.os.IBinder obj) {
            if (obj == null) return null;
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin instanceof IUserService) return (IUserService) iin;
            return new Proxy(obj);
        }

        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags)
                throws android.os.RemoteException {
            switch (code) {
                case INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                case TRANSACTION_destroy:
                    data.enforceInterface(DESCRIPTOR);
                    destroy();
                    reply.writeNoException();
                    return true;
                case TRANSACTION_exit:
                    data.enforceInterface(DESCRIPTOR);
                    exit();
                    reply.writeNoException();
                    return true;
                case TRANSACTION_execShell: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean result = execShell(data.readString());
                    reply.writeNoException();
                    reply.writeInt(result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_setUidFirewallRule: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean result = setUidFirewallRule(data.readInt(), data.readInt() != 0);
                    reply.writeNoException();
                    reply.writeInt(result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_ping: {
                    data.enforceInterface(DESCRIPTOR);
                    String pingResult = ping();
                    reply.writeNoException();
                    reply.writeString(pingResult);
                    return true;
                }
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IUserService {
            private final android.os.IBinder remote;

            Proxy(android.os.IBinder remote) {
                this.remote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return remote;
            }

            @Override
            public void destroy() throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    remote.transact(TRANSACTION_destroy, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void exit() throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    remote.transact(TRANSACTION_exit, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean execShell(String command) throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(command);
                    remote.transact(TRANSACTION_execShell, data, reply, 0);
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean setUidFirewallRule(int uid, boolean block) throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(uid);
                    data.writeInt(block ? 1 : 0);
                    remote.transact(TRANSACTION_setUidFirewallRule, data, reply, 0);
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public String ping() throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    remote.transact(TRANSACTION_ping, data, reply, 0);
                    reply.readException();
                    return reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }

        static final int TRANSACTION_destroy = 16777114;
        static final int TRANSACTION_exit = 1;
        static final int TRANSACTION_execShell = 2;
        static final int TRANSACTION_setUidFirewallRule = 3;
        static final int TRANSACTION_ping = 4;
    }
}

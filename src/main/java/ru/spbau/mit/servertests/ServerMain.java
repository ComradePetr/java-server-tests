package ru.spbau.mit.servertests;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.servertests.architecture.RunnerType;
import ru.spbau.mit.servertests.architecture.ServerType;
import ru.spbau.mit.servertests.servers.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Основной класс сервера.
 * Слушает порт Config.MAIN_SERVER_PORT для получения управляющих команд:
 * запуск сервера-обработчика (запускается в отдельном потоке),
 * его остановка (отправляется статистика - среднее время на обработку запроса и клиента).
 * Для поддержания сервера-обработчика,
 * обрабатывающего клиентов в разных процессах, есть возможность запустить новый сервер, остановив этот (spawn).
 * Новый сервер запускает тот же сервер-обработчик сразу (не дожидаясь управляющей команды),
 * тип сервера-обработчика передаётся как параметр командной строки.
 */
public final class ServerMain {
    public static final int REQUEST_OPEN = 0;
    public static final int REQUEST_CLOSE = 1;
    public static final int CONFIRM_SIGNAL = 17;
    public static final ExecutorService
            CACHED_THREAD_POOL = Executors.newCachedThreadPool(),
            FIXED_THREAD_POOL = Executors.newFixedThreadPool(Config.FIXED_THREAD_POOL_SIZE);
    public static int processNumber = 1;

    private static final Logger LOG = LogManager.getLogger(ServerMain.class);
    private static ServerSocket serverSocket;
    private static Server server;
    private static ServerType serverType;
    private static RunnerType runnerType;
    private static boolean closed = false;

    public static void main(String[] args) throws IOException {
        if (args.length != 0) {
            processNumber = Integer.parseInt(args[0]);
            serverType = ServerType.values()[Integer.parseInt(args[1])];
            runnerType = RunnerType.values()[Integer.parseInt(args[2])];
            run();
        }

        LOG.info("I will occupy {}", Config.MAIN_SERVER_PORT);
        try (ServerSocket serverSocket = new ServerSocket(Config.MAIN_SERVER_PORT)) {
            synchronized (ServerMain.class) {
                ServerMain.serverSocket = serverSocket;
            }

            while (!closed) {
                try (Socket socket = serverSocket.accept();
                     DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                     DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
                    int requestType = dataInputStream.readInt();
                    LOG.info("type = {}", requestType);
                    if (requestType == REQUEST_OPEN) {
                        serverType = ServerType.values()[dataInputStream.readInt()];
                        runnerType = RunnerType.values()[dataInputStream.readInt()];
                        LOG.info("server = {}, runner = {}", serverType, runnerType);

                        stop();
                        run();
                        dataOutputStream.writeInt(CONFIRM_SIGNAL);
                    } else {
                        LOG.info("{} {}", server.requestHandleTime(), server.clientHandleTime());
                        dataOutputStream.writeDouble(server.requestHandleTime());
                        dataOutputStream.writeDouble(server.clientHandleTime());
                        stop();
                    }
                    dataOutputStream.flush();
                } catch (SocketException e) {
                    break;
                } catch (IOException e) {
                    LOG.error(Throwables.getStackTraceAsString(e));
                }
            }
        } catch (IOException e) {
            LOG.error(Throwables.getStackTraceAsString(e));
        }
    }

    public static void spawn() throws URISyntaxException, IOException {
        LOG.info("Spawn");
        synchronized (ServerMain.class) {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    LOG.error(Throwables.getStackTraceAsString(e));
                }
                LOG.info("Port just was freed");
            } else {
                LOG.info("Port is already free");
            }
            closed = true;
        }

        stop();

        int newProcessNumber = processNumber + 1;
        ProcessBuilder pb = new ProcessBuilder(
                "cmd", "/c", "start", "java", "-cp",
                ServerMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(),
                ServerMain.class.getCanonicalName(),
                String.valueOf(newProcessNumber),
                String.valueOf(serverType.ordinal()),
                String.valueOf(runnerType.ordinal())
        );

        File workingDirectory = Paths.get("..", String.format("server-%d", newProcessNumber)).toFile();
        if (!workingDirectory.mkdirs()) {
            throw new IOException("Can't create directory structure for new server's process");
        }
        pb.directory(workingDirectory);

        LOG.info(Joiner.on(" ").join(pb.command()));
        pb.start();
    }

    private ServerMain() {
    }

    private static void run() {
        server = serverType.constructor.apply(runnerType);
        new Thread(server::run).start();
    }

    private static void stop() {
        LOG.info("I'm stopping");
        if (server != null) {
            server.close();
        }
    }
}

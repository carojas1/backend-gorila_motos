package com.projectBackend.GMotors.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "udp.broadcast.enabled", havingValue = "true", matchIfMissing = false)

import java.net.*;
import java.time.LocalTime;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

@Component
public class UdpBroadcastService {

	private static final String SERVICE_NAME = "MiBackendApp";
	private static final int UDP_PORT = 54545;

	@Value("${server.port:8080}")
	private int serverPort;

	private DatagramSocket socket;
	private Timer timer;

	private InetAddress broadcastAddress;
	private String localIp;

	@PostConstruct
	public void startBroadcast() {
		try {
			resolveNetwork();

			socket = new DatagramSocket();
			socket.setBroadcast(true);

			timer = new Timer(true);
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					try {
						String payload = String.format("{\"service\":\"%s\",\"ip\":\"%s\",\"port\":%d}", SERVICE_NAME,
								localIp, serverPort);

						byte[] data = payload.getBytes();

						DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, UDP_PORT);

						socket.send(packet);

						System.out.println("📡 [" + LocalTime.now() + "] UDP Broadcast: " + payload);

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, 0, 5000);

			System.out.println("✅ UDP Broadcast Service iniciado en " + broadcastAddress);

		} catch (Exception e) {
			System.err.println("❌ Error iniciando UDP Broadcast: " + e.getMessage());
		}
	}

	private void resolveNetwork() throws SocketException {
		NetworkInterface fallback = null;
		InterfaceAddress fallbackAddress = null;

		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

		while (interfaces.hasMoreElements()) {
			NetworkInterface ni = interfaces.nextElement();

			if (!ni.isUp() || ni.isLoopback())
				continue;

			for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
				InetAddress addr = ia.getAddress();

				if (!(addr instanceof Inet4Address) || ia.getBroadcast() == null)
					continue;

				String name = ni.getDisplayName().toLowerCase();

				// 🎯 PRIORIDAD: Wi-Fi real
				if (name.contains("wi-fi") || name.contains("wifi") || name.contains("wlan")) {
					localIp = addr.getHostAddress();
					broadcastAddress = ia.getBroadcast();
					System.out.println("📶 Interfaz Wi-Fi seleccionada: " + ni.getDisplayName());
					return;
				}

				// guardar como fallback (ej. VirtualBox)
				if (fallback == null) {
					fallback = ni;
					fallbackAddress = ia;
				}
			}
		}

		if (fallback != null) {
			localIp = fallbackAddress.getAddress().getHostAddress();
			broadcastAddress = fallbackAddress.getBroadcast();
			System.out.println("⚠ Usando interfaz fallback: " + fallback.getDisplayName());
			return;
		}

		throw new IllegalStateException("No se encontró interfaz de red válida");
	}

	@PreDestroy
	public void stopBroadcast() {
		if (timer != null)
			timer.cancel();
		if (socket != null && !socket.isClosed())
			socket.close();
	}
}

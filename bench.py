import http.client, threading, time

HOST, PORT = "localhost", 8080
PATH = "/hGymsl"
CONC = 50
PER = 40

lat = []
lock = threading.Lock()

def worker():
    conn = http.client.HTTPConnection(HOST, PORT)
    local = []
    for _ in range(PER):
        t0 = time.perf_counter()
        conn.request("GET", PATH)
        conn.getresponse().read()
        local.append((time.perf_counter() - t0) * 1000)
    conn.close()
    with lock:
        lat.extend(local)

for _ in range(3):
    c = http.client.HTTPConnection(HOST, PORT)
    c.request("GET", PATH); c.getresponse().read(); c.close()

ts = [threading.Thread(target=worker) for _ in range(CONC)]
t0 = time.perf_counter()
for t in ts: t.start()
for t in ts: t.join()
el = time.perf_counter() - t0

lat.sort()
n = len(lat)
print("requests : %d" % n)
print("elapsed  : %.2f s" % el)
print("QPS      : %.0f" % (n / el))
print("avg      : %.1f ms" % (sum(lat) / n))
print("p50      : %.1f ms" % lat[n // 2])
print("p95      : %.1f ms" % lat[int(n * 0.95)])
print("p99      : %.1f ms" % lat[int(n * 0.99)])
import time
import urllib.request
import urllib.error
from concurrent.futures import ThreadPoolExecutor

URL = "http://127.0.0.1:8080/api/shorten?longUrl=https://www.baidu.com"
TOTAL = 200
CONC = 50

opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))

def hit(i):
    req = urllib.request.Request(URL, data=b"", method="POST")
    try:
        with opener.open(req, timeout=5) as r:
            return r.status
    except urllib.error.HTTPError as e:
        return e.code
    except Exception as e:
        return ("ERR", type(e).__name__, str(e)[:120])

t0 = time.time()
with ThreadPoolExecutor(max_workers=CONC) as ex:
    results = list(ex.map(hit, range(TOTAL)))
elapsed = time.time() - t0

codes = [r for r in results if isinstance(r, int)]
errs  = [r for r in results if not isinstance(r, int)]
ok = codes.count(200)
blocked = codes.count(429)
other = len(codes) - ok - blocked

print()
print("total     :", TOTAL, " concurrency:", CONC)
print("elapsed   : %.2f s" % elapsed)
print("allowed   :", ok)
print("limited   :", blocked, "(429)")
print("http-other:", other)
print("errors    :", len(errs))
print("reject    : %.1f%%" % (100.0 * blocked / TOTAL))
print("qps       : %d" % (TOTAL / elapsed))
for e in errs[:3]: print("  ", e)
print()
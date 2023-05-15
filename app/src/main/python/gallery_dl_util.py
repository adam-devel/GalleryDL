from gallery_dl import config, job
from contextlib import redirect_stdout
import io


def download(url, uid):
    config.load()
    config.set(("extractor",), "base-directory", "/storage/emulated/0/Download/GalleryDL/Image/")
    job.DownloadJob(url).run()
    print(f"--FINISHED DOWNLOADING {uid}--")

def metadata(url):
    f = io.StringIO()
    with redirect_stdout(f):
        job.UrlJob(url).run()
    s = f.getvalue()
    return len(s.split("\n"))
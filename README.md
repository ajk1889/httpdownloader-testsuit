# httpdownloader-testsuit
A test server for HTTP Downloader

Download link = https://github.com/ajk1889/httpdownloader-testsuit/releases/

## Usage
1. Start server using `java -jar TestSuit.jar`
2. Type one of the available commands and press Enter to tweek Server parameters
3. Go to browser & type `localhost:1234`

## Available commands
#### htdocs=[/full/path/to/folder]
If htdocs is set, any request to `localhost:1234` will display contents of that folder. Enter `htdocs=` as input to disable this. 
On clicking an item, any file will be downloaded irrespective of their type (including HTML files) and any folder will display its contents. 
In folder display, red colour indicates folder & black is file. Ongoing downloads **won't** be affected if you change this value
#### path123=[path/to/123/file]
If path123 is set, any request to that file will download 123 file. Even if `htdocs` is set & such a file already exists in it. 
By default `path123` is set to `/a.txt`. `path123` is mandatory & cannot be disabled. Change to different path if it conflicts with existing file
Ongoing downloads **won't** be affected if you change this value
#### ping=[number]
Number of milliseconds, server should delay the response. Ongoing downloads **won't** be affected if you change this value
#### sleep=[number]
Number of milliseconds server should sleep between writing each buffer. **Ongoing downloads _will_ be affected if you change this value**
#### bfrsize=[number]
The number of bytes each buffer should hold. Ongoing downloads **won't** be affected if you change this value.
#### cookie=[string]
Server will respond with error 403 if cookie is not sent along with request. 
The response will always contain `Set-Cookie` header if this is set.
Enter `cookie=` as input to disable cookie verification. Ongoing downloads **won't** be affected if you change this value.
#### nolength=[boolean]
Server will not return file size (Content-Length and Content-Range) along with the response. Ongoing downloads **won't** be affected if you change this value
#### lengthonly=[boolean]
Server will not accept Content-Range requests. File will always be returned from beginning. Ongoing downloads **won't** be affected if you change this value
#### logging=[boolean]
Server will stop verbose logging. Useful when multiple concurrent connections are being made & you have to tweek parameters frequently.

# s3-beamer

`s3-beamer` is a Clojure/Clojurescript library designed to help you upload files
from the browser to S3 (CORS upload).  This library supports all s3 regions (using AWS auth V4).

[](dependency)
```clojure
[whamtet/s3-beamer "0.6.0-SNAPSHOT"] ;; latest release
```
[](/dependency)

## Usage

To upload files directly to S3 you need to send special request
parameters that are based on your AWS credentials, the file name, mime
type, date etc.
Since we **don't want to store our credentials in the client** these
parameters need to be generated on the server side.
For this reason this library consists of two parts:

1. A pluggable route that will send back the required parameters for a
   given file-name & mime-type
2. A client-side core.async pipeline setup that will retrieve the
   special parameters for a given File object, upload it to S3 and
   report back to you

### 1. Enable CORS on your S3 bucket

Please follow Amazon's [official documentation](http://docs.aws.amazon.com/AmazonS3/latest/dev/cors.html).  An example permission document is

```xml
<CORSConfiguration>
 <CORSRule>
   <AllowedOrigin>http://www.example1.com</AllowedOrigin>

   <AllowedMethod>PUT</AllowedMethod>
   <AllowedMethod>POST</AllowedMethod>
   <AllowedMethod>DELETE</AllowedMethod>

   <AllowedHeader>*</AllowedHeader>
 </CORSRule>
```

### 2. Plug-in the route to sign uploads

```clj
(ns your.server
  (:require [s3-beam.handler :as s3b]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]))

(def bucket "your-bucket")
(def aws-zone "eu-west-1")
(def access-key "your-aws-access-key")
(def secret-key "your-aws-secret-key")

(defroutes routes
  (resources "/")
  (GET "/sign" {params :params} (s3b/s3-sign
                                  params
                                  {:bucket bucket
                                   :aws-zone aws-zone
                                   :aws-access-key access-key
                                   :aws-secret-key secret-key})))
```

If you want to use a route different than `/sign`, define it in the
handler, `(GET "/my-cool-route" ...)`, and then pass it in the options
map to `upload` in the frontend.

If you are serving your S3 bucket with CloudFront, or another CDN/proxy, you can pass
`upload-url` as a fifth parameter to `s3-sign`, so that the ClojureScript client is directed
to upload through this bucket. You still need to pass the bucket name, as the policy that is
created and signed is based on the bucket name.

### 3. Integrate the upload pipeline into your frontend

```clojure
(ns my-ns
  (:require [s3-beamer.client :as s3]))

(s3/upload
  {:server-url "/sign"
   :sign-params {"my_param" "my_value"}
   :file file
   :success-fn success-fn
   :error-fn error-fn
   :upload-listener upload-listener
   })

```

`:server-url` is the route defined in step 2.  Defaults to "/sign" if not provided
`:sign-params` includes additional get parameters that will be passed to `:server-url`.
`:file-to-upload` should be the file object obtained from a file input selector
`:upload-listener` will recieve partial upload events (useful for large files)

Please include jQuery on the frontend.
